import os
from fastapi import FastAPI, Body, HTTPException
import chromadb

# ✅ 호환성 보장을 위해 community 버전 사용 (Search 에러 방지)
from langchain_upstage import ChatUpstage, UpstageEmbeddings
from langchain_community.vectorstores import Chroma
from langchain_community.document_loaders import DirectoryLoader, Docx2txtLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter

# LangChain RAG 체인 관련
from langchain.chains import create_retrieval_chain
from langchain.chains.combine_documents import create_stuff_documents_chain
from langchain_core.prompts import ChatPromptTemplate

app = FastAPI()

# [설정] 환경변수 로드
# 도커 환경에서는 'chroma'라는 서비스 이름을, 로컬 테스트 시에는 '127.0.0.1'을 사용
CHROMA_HOST = os.getenv("CHROMA_HOST", "127.0.0.1")
CHROMA_PORT = int(os.getenv("CHROMA_PORT", "4008"))
UPSTAGE_API_KEY = os.getenv("UPSTAGE_API_KEY")

if not UPSTAGE_API_KEY:
    raise RuntimeError("UPSTAGE_API_KEY 환경변수가 설정되어 있지 않습니다.")

# [모델 초기화]
llm = ChatUpstage(model="solar-pro", api_key=UPSTAGE_API_KEY)
embeddings = UpstageEmbeddings(model="solar-embedding-1-large", api_key=UPSTAGE_API_KEY)

# [ChromaDB 연결]
client = chromadb.HttpClient(host=CHROMA_HOST, port=CHROMA_PORT)
vectorstore = Chroma(
    client=client,
    collection_name="law_docs",
    embedding_function=embeddings,
)

# [서버 시작 시 자동 학습]
@app.on_event("startup")
async def load_documents():
    docs_dir = "./docs"
    if not (os.path.exists(docs_dir) and os.listdir(docs_dir)):
        print("ℹ️ docs 폴더가 비어있어 학습을 건너뜁니다.")
        return

    try:
        # 중복 학습 방지: 데이터가 이미 있으면 중단
        count = vectorstore._collection.count()
        if count > 0:
            print(f"ℹ️ 이미 {count}개의 벡터 데이터가 존재합니다. 자동 학습을 건너뜁니다.")
            return
    except Exception as e:
        print(f"⚠️ Chroma 연결 확인 중 이슈 발생(무시 가능): {e}")

    print("🚀 문서 학습 시작 (docx2txt 사용)...")
    loader = DirectoryLoader(docs_dir, glob="**/*.docx", loader_cls=Docx2txtLoader)
    docs = loader.load()

    splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=100)
    splits = splitter.split_documents(docs)

    vectorstore.add_documents(splits)
    print(f"✅ {len(docs)}개 문서, {len(splits)}개 청크 저장 완료!")

# [RAG 프롬프트 및 체인 설정]
system_prompt = (
    "당신은 법률/계약 전문 AI 보조원입니다. 제공된 문맥(Context)을 바탕으로 답변하세요. "
    "모르는 내용은 지어내지 마세요.\n\n{context}"
)

prompt = ChatPromptTemplate.from_messages([
    ("system", system_prompt),
    ("human", "{input}"),
])

# 문서 결합 및 리트리벌 체인 생성
combine_docs_chain = create_stuff_documents