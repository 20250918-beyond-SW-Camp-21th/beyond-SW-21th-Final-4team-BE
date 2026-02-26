import os
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from langchain_upstage import UpstageEmbeddings, ChatUpstage
from langchain_chroma import Chroma
from langchain.chains.combine_documents import create_stuff_documents_chain
from langchain_core.prompts import ChatPromptTemplate
from langchain.chains import create_retrieval_chain
import chromadb

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

UPSTAGE_API_KEY = os.getenv("UPSTAGE_API_KEY")
if not UPSTAGE_API_KEY:
    logger.error("UPSTAGE_API_KEY is missing! The application will exit.")
    raise ValueError("UPSTAGE_API_KEY environment variable is required.")

CHROMA_HOST = os.getenv("CHROMA_HOST", "localhost")
CHROMA_PORT = int(os.getenv("CHROMA_PORT", "8000"))

vectorstore = None
rag_chain = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    global vectorstore, rag_chain
    try:
        client = chromadb.HttpClient(host=CHROMA_HOST, port=CHROMA_PORT)
        
        embeddings = UpstageEmbeddings(api_key=UPSTAGE_API_KEY, model="embedding-v2")
        
        vectorstore = Chroma(
            client=client,
            collection_name="law_docs",
            embedding_function=embeddings
        )

        llm = ChatUpstage(api_key=UPSTAGE_API_KEY)
        prompt = ChatPromptTemplate.from_template("""
        Answer the following question based only on the provided context:
        <context>
        {context}
        </context>
        Question: {input}
        """)

        combine_docs_chain = create_stuff_documents_chain(llm, prompt)
        retriever = vectorstore.as_retriever()
        rag_chain = create_retrieval_chain(retriever, combine_docs_chain)
        
        logger.info("RAG System successfully initialized.")
        
    except (ConnectionError, ValueError) as ce: 
        logger.error(f"Connection/Config Error during init: {ce}")
    except Exception as e: 
        logger.exception(f"Unexpected error during initialization: {e}")
    
    yield

app = FastAPI(lifespan=lifespan)

class Query(BaseModel):
    question: str

@app.post("/ask")
async def ask_question(query: Query):
    if not rag_chain:
        raise HTTPException(status_code=503, detail="RAG system is not initialized")
    
    try:
        response = rag_chain.invoke({"input": query.question})
        return {"answer": response["answer"]}
    except Exception as e:
        # 'from e'를 사용하여 원본 에러 정보를 보존
        logger.exception("Error during RAG invocation")
        raise HTTPException(status_code=500, detail="Internal Server Error") from e