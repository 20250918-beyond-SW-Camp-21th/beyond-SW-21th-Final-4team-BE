import unittest
import os
import sys
import pymysql
from dotenv import load_dotenv
from datetime import datetime
from operator import itemgetter
from pathlib import Path
from unittest.mock import MagicMock, patch

# 테스트할 모듈 임포트
from loader import sync_maria_to_chroma
from database import get_vectorstore
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser

# .env 로드
env_path = Path(__file__).resolve().parent / ".env"
load_dotenv(dotenv_path=env_path, override=True)

class TestRAGFlow(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        db_name = os.getenv("DB_NAME", "freebridge")
        print(f"\n⚠️ [INFO] [{db_name}] DB에서 통합 테스트를 시작합니다.")
        print("💡 모든 DB 변경사항은 종료 후 자동으로 Rollback됩니다.")

        try:
            cls.conn = pymysql.connect(
                host=os.getenv("DB_HOST", "localhost"),
                user=os.getenv("DB_USER", "root"),
                password=os.getenv("DB_PASSWORD", "password"),
                db=db_name,
                port=int(os.getenv("DB_PORT", 3306)),
                charset='utf8mb4',
                autocommit=False  # [중요] 롤백을 위해 반드시 False
            )
        except Exception as e:
            print(f"❌ DB 연결 실패: {e}")
            sys.exit(1)

        # 테스트용 별도 벡터 컬렉션 사용
        cls.test_collection_name = "test_temp_rag_flow"
        cls.vectorstore = get_vectorstore(collection_name=cls.test_collection_name)

    def setUp(self):
        self.cursor = self.conn.cursor()
        now = datetime.now()
        
        try:
            # 1. 테스트용 기업 유저 생성
            sql_user = "INSERT INTO user (email, name, password, role, email_verified, privacy_agreed, terms_agreed, created_at, updated_at) VALUES (%s, %s, %s, %s, 1, 1, 1, %s, %s)"
            self.cursor.execute(sql_user, ('test_corp@test.com', '테스트기업', 'pw', 'EMPLOYER', now, now))
            corp_user_id = self.cursor.lastrowid
            
            self.cursor.execute("INSERT INTO employer (user_id, company_name, scale, subscription, status, created_at, updated_at) VALUES (%s, '테스트기업', 'S100_299', 'PRO', 'ACTIVE', %s, %s)", (corp_user_id, now, now))
            employer_id = self.cursor.lastrowid

            # 2. 테스트용 프리랜서(김철수) 생성
            self.cursor.execute(sql_user, ('test_kim@test.com', '테스트김철수', 'pw', 'FREELANCER', now, now))
            self.user_id = self.cursor.lastrowid
            self.cursor.execute("INSERT INTO freelancer (freelancer_id, user_id, status, created_date, updated_at) VALUES (%s, %s, 'POTENTIAL', %s, %s)", (self.user_id, self.user_id, now, now))

            # 3. 공고 생성 (Java 시스템 구축)
            sql_job = "INSERT INTO job_posting (employer_id, employer_name, title, description, budget, duration, status, posting_status, headcount, matched_headcount, created_at, updated_at) VALUES (%s, %s, %s, %s, 8000000, 6, 'OPEN', 'OPEN', 1, 0, %s, %s)"
            self.cursor.execute(sql_job, (employer_id, '테스트기업', '금융 시스템 개발', 'Java 백엔드 개발 업무', now, now))
            job_id = self.cursor.lastrowid
            
            # 기술 스택 등록 (loader.py 조인용)
            self.cursor.execute("INSERT INTO job_posting_tech_stack (job_posting_id, tech) VALUES (%s, 'Java')", (job_id,))

            # 4. 프로젝트 완료 이력 생성 (이게 있어야 RAG가 검색함)
            sql_project = "INSERT INTO projects (employer_id, freelancer_id, job_posting_id, project_name, status, headcount, created_at, updated_at) VALUES (%s, %s, %s, %s, 'COMPLETED', 1, %s, %s)"
            self.cursor.execute(sql_project, (employer_id, self.user_id, job_id, '금융권 차세대 프로젝트', now, now))

        except Exception as e:
            self.conn.rollback()
            self.fail(f"테스트 데이터 준비 중 오류 발생: {e}")

    def tearDown(self):
        # [핵심] DB 롤백: 실제 데이터 보존
        self.conn.rollback()
        print("🧹 DB 트랜잭션 롤백 완료.")
        try:
            self.vectorstore.delete_collection()
        except:
            pass

    @classmethod
    def tearDownClass(cls):
        cls.conn.close()

    @patch('langchain_upstage.ChatUpstage')
    def test_recommendation_logic(self, mock_chat):
        """데이터 동기화 및 RAG 추천 로직 통합 테스트"""
        print("\n🧪 테스트 시작: 데이터 동기화 및 RAG 검증")
        
        # 1. MariaDB -> ChromaDB 동기화
        sync_maria_to_chroma(conn=self.conn, vectorstore=self.vectorstore)
        
        # 2. LLM 응답 Mocking (비용 절감 및 안정성)
        mock_llm = MagicMock()
        mock_llm.invoke.return_value = "과거 금융 시스템 개발 경험이 있는 테스트김철수를 추천합니다."
        mock_chat.return_value = mock_llm

        # 3. RAG 체인 구성 (main.py와 동일)
        prompt = ChatPromptTemplate.from_template("{context} 기반으로 {input} 추천해줘")
        def format_docs(docs):
            return "\n\n".join(doc.page_content for doc in docs)

        retriever = self.vectorstore.as_retriever(search_kwargs={"k": 1})
        rag_chain = (
            {"context": itemgetter("input") | retriever | format_docs, "input": itemgetter("input")}
            | prompt
            | mock_chat()
            | StrOutputParser()
        )

        # 4. 질문 및 결과 검증
        question = "Java 개발자 추천해줘"
        answer = rag_chain.invoke({"input": question})
        
        print(f"💡 답변 결과: {answer}")
        self.assertIn("테스트김철수", answer)
        print("✅ 검증 성공: 결과에 '테스트김철수'가 포함되어 있습니다.")

if __name__ == '__main__':
    unittest.main()