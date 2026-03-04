import os
from dotenv import load_dotenv
import chromadb
from langchain_chroma import Chroma
from langchain_upstage import UpstageEmbeddings

load_dotenv()

host = os.getenv("CHROMA_HOST", "localhost")
port = int(os.getenv("CHROMA_PORT", "8000"))
api_key = os.getenv("UPSTAGE_API_KEY")

if not api_key:
    raise ValueError("UPSTAGE_API_KEY is missing!")

embeddings = UpstageEmbeddings(api_key=api_key, model="solar-embedding-1-large")

client = chromadb.HttpClient(host=host, port=port)

vector_db = Chroma(
    client=client,
    collection_name="freebridge_main",
    embedding_function=embeddings
)

def get_vectorstore():
    return vector_db