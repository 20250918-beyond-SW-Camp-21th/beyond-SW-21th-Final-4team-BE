import os
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from langchain_upstage import UpstageEmbeddings, ChatUpstage
from langchain_chroma import Chroma
from langchain.chains.combine_documents import create_stuff_documents_chain # 올바른 임포트
from langchain_core.prompts import ChatPromptTemplate
from langchain.chains import create_retrieval_chain
import chromadb

UPSTAGE_API_KEY = os.getenv("UPSTAGE_API_KEY")

vectorstore = None
rag_chain = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    global vectorstore, rag_chain
    try:
        client = chromadb.HttpClient(host='localhost', port=8000)
        embeddings = UpstageEmbeddings(api_key=UPSTAGE_API_KEY, model="embedding-v2")
        
        vectorstore = Chroma(
            client=client,
            collection_name="law_docs",
            embedding_function=embeddings
        )
        
        res = vectorstore.get(limit=1)
        if not res['ids']:
            print("Warning: Vectorstore is empty.")

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
        
    except Exception as e:
        print(f"Initialization Error: {e}")
    
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
        raise HTTPException(status_code=500, detail=str(e))