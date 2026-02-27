import os
import sys
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from operator import itemgetter
from dotenv import load_dotenv

from langchain_upstage import ChatUpstage
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser
from database import get_vectorstore

load_dotenv()

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

rag_chain = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    global rag_chain
    try:
        vectorstore = get_vectorstore()
        llm = ChatUpstage(api_key=os.getenv("UPSTAGE_API_KEY"))
        
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

        retriever = vectorstore.as_retriever(search_kwargs={"k": 3})
        rag_chain = (
            {"context": itemgetter("input") | retriever | format_docs, "input": itemgetter("input")}
            | prompt
            | llm
            | StrOutputParser()
        )
        
        logger.info("RAG System Initialized.")
    except Exception as e:
        logger.error(f"Critical Init Error: {e}")
        # [개선] 초기화 실패 시 서버 구동을 중단하여 잘못된 상태로 서비스되는 것을 방지
        sys.exit(1)
    yield

app = FastAPI(lifespan=lifespan)

class Query(BaseModel):
    question: str

@app.post("/api/ai/recommend")
async def recommend(query: Query):
    if not rag_chain:
        raise HTTPException(status_code=503, detail="System not ready")
    try:
        # [개선] 비동기 ainvoke 사용으로 논블로킹 처리
        response = await rag_chain.ainvoke({"input": query.question})
        return {"answer": response}
    except Exception as e:
        logger.exception("Error during invocation")
        raise HTTPException(status_code=500, detail="Internal Error") from e