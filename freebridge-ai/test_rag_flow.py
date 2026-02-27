import unittest
import os
import sys
import pymysql
from dotenv import load_dotenv
from datetime import datetime
from operator import itemgetter
from loader import sync_maria_to_chroma
from database import get_vectorstore
from langchain_upstage import ChatUpstage
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser

load_dotenv(override=True)

class TestRAGFlow(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        db_name = os.getenv("DB_NAME", "freebridge")
        # [개선] 안전장치: 공용 DB일 경우 경고 출력 및 트랜잭션 모드 강제
        if "test" not in db_name.lower():
            print(f"⚠️ 경고: 공용 DB({db_name})에서 테스트를 진행합니다. 데이터는 종료 후 롤백됩니다.")

        try:
            cls.conn = pymysql.connect(
                host=os.getenv("DB_HOST", "localhost"),
                user=os.getenv("DB_USER", "root"),
                password=os.getenv("DB_PASSWORD", "password"),
                db=db_name,
                port=int(os.getenv("DB_PORT", 3306)),
                charset='utf8mb4',
                autocommit=False  # [개선] 롤백을 위해 autocommit을 반드시 꺼야 함
            )
        except Exception as e:
            print(f"❌ DB 연결 실패: {e}")
            sys.exit(1)

        cls.test_collection_name = "test_freelancer_experience"
        cls.vectorstore = get_vectorstore(collection_name=cls.test_collection_name)

    @classmethod
    def tearDownClass(cls):
        cls.conn.close()

    def setUp(self):
        self.cursor = self.conn.cursor()
        now = datetime.now()
        
        # [개선] 전체 삭제 대신 테스트용 데이터만 삽입 (기존 데이터와 충돌 방지를 위해 고유 이메일 사용)
        sql_user = "INSERT INTO user (email, name, password, role, email_verified, privacy_agreed, terms_agreed, created_at, updated_at) VALUES (%s, %s, %s, %s, 1, 1, 1, %s, %s)"
        self.cursor.execute(sql_user, ('test_kim@test.com', '테스트김철수', 'pw', 'FREELANCER', now, now))
        self.user_id = self.cursor.lastrowid
        
        # ... (나머지 프로필/공고/프로젝트 삽입 로직 동일하게 유지)

    def tearDown(self):
        # [개선] 핵심: DB에 커밋하지 않고 롤백하여 테스트 데이터만 삭제
        self.conn.rollback() 
        try:
            self.vectorstore.delete_collection()
        except Exception as e:
            print(f"⚠️ ChromaDB 정리 중 알림: {e}")

    def test_recommendation_logic(self):
        """통합 테스트: 데이터 동기화 및 RAG 추천 검증"""
        # 트랜잭션 내의 데이터를 읽어오기 위해 현재 conn을 전달
        sync_maria_to_chroma(conn=self.conn, vectorstore=self.vectorstore)
        
        llm = ChatUpstage(api_key=os.getenv("UPSTAGE_API_KEY"))
        # ... (체인 구성 및 assert 로직 기존과 동일하게 유지)
        
        question = "금융권 Java 백엔드 개발자 추천해줘"
        response = ChatPromptTemplate.from_template("...").pipe(llm).invoke({"input": question}) # 예시 구조
        self.assertIn("테스트김철수", str(response))

if __name__ == '__main__':
    unittest.main()