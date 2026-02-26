import os
from dotenv import load_dotenv
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from operator import itemgetter

load_dotenv()

# LangChain 버전에 따른 호환성 보장을 위한 import
from langchain_upstage import ChatUpstage
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser
from langchain_core.runnables import RunnablePassthrough
from database import get_vectorstore


logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

rag_chain = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    global rag_chain
    try:
        vectorstore = get_vectorstore()
        llm = ChatUpstage(api_key=os.getenv("UPSTAGE_API_KEY"))
        
        # 추천을 위한 프롬프트로 수정
        prompt = ChatPromptTemplate.from_template("""
        당신은 IT 프리랜서 매칭 전문가입니다.
        새로운 프로젝트 공고 내용(Question)이 주어지면, 과거에 수행된 유사한 프로젝트 정보(Context)를 참고하여 가장 적합한 프리랜서를 추천하세요.
        추천 이유는 과거에 수행한 프로젝트의 내용과 기술 스택을 근거로 구체적으로 설명해야 합니다.
        
        <context>
        {context}
        </context>
        새로운 프로젝트 공고: {input}
        """)

        def format_docs(docs):
            return "\n\n".join(doc.page_content for doc in docs)

        retriever = vectorstore.as_retriever(search_kwargs={"k": 3}) # 유사도 높은 3명 추출
        rag_chain = (
            {"context": itemgetter("input") | retriever | format_docs, "input": itemgetter("input")}
            | prompt
            | llm
            | StrOutputParser()
        )
        
        logger.info("RAG System Initialized.")
    except Exception as e:
        logger.error(f"Init Error: {e}")
    yield

app = FastAPI(lifespan=lifespan)

class Query(BaseModel):
    question: str

@app.post("/api/ai/recommend") # 추천 전용 엔드포인트 예시
async def recommend(query: Query):
    if not rag_chain:
        raise HTTPException(status_code=503, detail="System not ready")
    try:
        response = rag_chain.invoke({"input": query.question})
        return {"answer": response}
    except Exception as e:
        logger.exception("Error during invocation")
        raise HTTPException(status_code=500, detail="Internal Error") from e