import os
from dotenv import load_dotenv
import chromadb
from langchain_chroma import Chroma
from langchain_upstage import UpstageEmbeddings

load_dotenv()

def get_vectorstore(collection_name="freelancer_experience"):
    # 환경변수 설정
    host = os.getenv("CHROMA_HOST", "localhost")
    port = int(os.getenv("CHROMA_PORT", "8000"))
    api_key = os.getenv("UPSTAGE_API_KEY")

    if not api_key:
        raise ValueError("UPSTAGE_API_KEY is missing!")

    client = chromadb.HttpClient(host=host, port=port)
    embeddings = UpstageEmbeddings(api_key=api_key, model="solar-embedding-1-large")
    
    return Chroma(
        client=client,
        collection_name=collection_name, # 컬렉션명 변경 (경험 기반)
        embedding_function=embeddings
    )