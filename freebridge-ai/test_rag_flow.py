import unittest
import os
import pymysql
from dotenv import load_dotenv
from datetime import datetime
from operator import itemgetter
from pathlib import Path

# 테스트할 모듈 임포트
from loader import sync_maria_to_chroma
from database import get_vectorstore
from langchain_upstage import ChatUpstage
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser
from langchain_core.runnables import RunnablePassthrough

# .env 파일을 명시적으로 로드 (현재 파일 위치 기준)
env_path = Path(__file__).resolve().parent / ".env"
load_dotenv(dotenv_path=env_path, override=True)

class TestRAGFlow(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        # 1. DB 연결
        host = os.getenv("DB_HOST", "localhost")
        port = int(os.getenv("DB_PORT", 3306))

        cls.conn = pymysql.connect(
            host=host,
            user=os.getenv("DB_USER", "root"),
            password=os.getenv("DB_PASSWORD", "password"),
            db=os.getenv("DB_NAME", "freebridge"),
            port=port,
            charset='utf8mb4',
            autocommit=False  # 트랜잭션 수동 제어 (Rollback을 위해 필수)
        )
        
        # 테이블이 없으면 생성하도록 로직 추가
        cls._recreate_tables()

        # 2. 테스트용 Vector Store 설정 (별도 컬렉션 사용)
        cls.test_collection_name = "test_freelancer_experience"
        cls.vectorstore = get_vectorstore(collection_name=cls.test_collection_name)

    @classmethod
    def _recreate_tables(cls):
        """테스트에 필요한 테이블을 삭제하고 다시 생성하여 스키마를 최신 상태로 보장합니다."""
        with cls.conn.cursor() as cursor:
            cursor.execute("SET FOREIGN_KEY_CHECKS = 0;")

            # 스키마 불일치 문제를 해결하기 위해 기존 테이블을 먼저 삭제합니다.
            cursor.execute("DROP TABLE IF EXISTS projects")
            cursor.execute("DROP TABLE IF EXISTS job_posting_tech_stack")
            cursor.execute("DROP TABLE IF EXISTS job_posting")
            cursor.execute("DROP TABLE IF EXISTS freelancer")
            cursor.execute("DROP TABLE IF EXISTS employer")
            cursor.execute("DROP TABLE IF EXISTS user")

            # User Table
            cursor.execute("""
            CREATE TABLE IF NOT EXISTS `user` (
              `user_id` bigint(20) NOT NULL AUTO_INCREMENT, `created_at` datetime(6) DEFAULT NULL, `updated_at` datetime(6) DEFAULT NULL,
              `email` varchar(255) NOT NULL, `email_verified` bit(1) NOT NULL, `name` varchar(255) NOT NULL,
              `password` varchar(255) NOT NULL, `privacy_agreed` bit(1) NOT NULL, `role` enum('EMPLOYER','FREELANCER') NOT NULL,
              `terms_agreed` bit(1) NOT NULL, PRIMARY KEY (`user_id`), UNIQUE KEY `UKob8kqyqqgmefl0aco34akdtpe` (`email`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
            """)

            # Employer Table
            cursor.execute("""
            CREATE TABLE IF NOT EXISTS `employer` (
              `employer_id` bigint(20) NOT NULL AUTO_INCREMENT, `company_name` varchar(100) NOT NULL, `created_at` datetime(6) NOT NULL,
              `description` text DEFAULT NULL, `industry` varchar(100) DEFAULT NULL, `location` varchar(100) DEFAULT NULL,
              `logo_url` varchar(500) DEFAULT NULL, `scale` enum('S1000_PLUS','S100_299','S10_29','S1_4','S300_999','S30_99','S5_9') NOT NULL,
              `subscription` enum('BASIC','PRIME','PRO') NOT NULL, `updated_at` datetime(6) NOT NULL, `user_id` bigint(20) NOT NULL,
              `website_url` varchar(100) DEFAULT NULL, `status` enum('ACTIVE','CONTRACTING','CONTRACT_EXPIRED','CONTRACT_IN_PROGRESS','CONTRACT_REVIEW','LEFT','POTENTIAL') NOT NULL,
              PRIMARY KEY (`employer_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
            """)

            # Freelancer Table
            cursor.execute("""
            CREATE TABLE IF NOT EXISTS `freelancer` (
              `freelancer_id` bigint(20) NOT NULL, `avatar_url` varchar(255) DEFAULT NULL, `average_rate` double DEFAULT NULL,
              `career_years` int(11) DEFAULT NULL, `communication` int(11) DEFAULT NULL, `dispute` int(11) DEFAULT NULL,
              `schedule_adherence` int(11) DEFAULT NULL, `created_date` datetime(6) DEFAULT NULL, `framework` int(11) DEFAULT NULL,
              `problem_solving` int(11) DEFAULT NULL, `programming` int(11) DEFAULT NULL, `grade` enum('INTERMEDIATE','JUNIOR','MASTER','SENIOR') DEFAULT NULL,
              `introduction` text DEFAULT NULL, `job` varchar(255) DEFAULT NULL, `portfolio_file_name` varchar(255) DEFAULT NULL,
              `portfolio_file_url` varchar(255) DEFAULT NULL, `portfolio_last_updated` datetime(6) DEFAULT NULL, `stat_chat` int(11) DEFAULT NULL,
              `stat_contact` int(11) DEFAULT NULL, `stat_contract` int(11) DEFAULT NULL, `top_percentile` int(11) DEFAULT NULL,
              `updated_at` datetime(6) DEFAULT NULL, `user_id` bigint(20) DEFAULT NULL, `wage` bigint(20) DEFAULT NULL,
              `conditions_type` varchar(255) DEFAULT NULL, `location` varchar(255) DEFAULT NULL, `start_date` date DEFAULT NULL,
              `work_style` varchar(255) DEFAULT NULL, `status` enum('CONTRACTING','CONTRACT_EXPIRED','CONTRACT_IN_PROGRESS','CONTRACT_REVIEW','LEFT','POTENTIAL') NOT NULL,
              PRIMARY KEY (`freelancer_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
            """)

            # Job Posting Table
            cursor.execute("""
            CREATE TABLE IF NOT EXISTS `job_posting` (
              `id` bigint(20) NOT NULL AUTO_INCREMENT, `budget` bigint(20) NOT NULL, `created_at` datetime(6) NOT NULL,
              `description` tinytext NOT NULL, `duration` int(11) NOT NULL, `employer_id` bigint(20) NOT NULL,
              `employer_name` varchar(255) NOT NULL, `status` enum('CLOSED','COMPLETED','IN_PROGRESS','OPEN') NOT NULL,
              `title` varchar(255) NOT NULL, `updated_at` datetime(6) NOT NULL, `posting_status` enum('CLOSED','COMPLETED','IN_PROGRESS','OPEN') NOT NULL,
              `headcount` int(11) NOT NULL, `matched_headcount` int(11) NOT NULL, PRIMARY KEY (`id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
            """)

            # Job Posting Tech Stack Table
            cursor.execute("""
            CREATE TABLE IF NOT EXISTS `job_posting_tech_stack` (
              `job_posting_id` bigint(20) NOT NULL,
              `tech` varchar(255) NOT NULL,
              KEY `FKpa850j5n12naa1nrdrrlxpqm5` (`job_posting_id`),
              CONSTRAINT `FKpa850j5n12naa1nrdrrlxpqm5` FOREIGN KEY (`job_posting_id`) REFERENCES `job_posting` (`id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
            """)

            # Projects Table
            cursor.execute("""
            CREATE TABLE IF NOT EXISTS `projects` (
              `id` bigint(20) NOT NULL AUTO_INCREMENT,
              `created_at` datetime(6) NOT NULL,
              `employer_id` bigint(20) NOT NULL,
              `end_date` date DEFAULT NULL,
              `freelancer_id` bigint(20) NOT NULL,
              `project_name` varchar(255) NOT NULL,
              `start_date` date DEFAULT NULL,
              `status` enum('CANCELLED','COMPLETED','IN_PROGRESS') NOT NULL,
              `updated_at` datetime(6) NOT NULL,
              `job_posting_id` bigint(20) NOT NULL,
              `headcount` int(11) NOT NULL,
              PRIMARY KEY (`id`),
              UNIQUE KEY `uk_project_job_posting_freelancer` (`job_posting_id`,`freelancer_id`),
              CONSTRAINT `FKnh1ni5vol0s1d1cvmx82itb51` FOREIGN KEY (`job_posting_id`) REFERENCES `job_posting` (`id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
            """)

            cursor.execute("SET FOREIGN_KEY_CHECKS = 1;")
        cls.conn.commit()

    @classmethod
    def tearDownClass(cls):
        # 요청에 따라, 테스트 후 테이블을 삭제하지 않도록 주석 처리합니다.
        # DB 상태를 직접 확인하고 싶을 때 유용합니다.
        # with cls.conn.cursor() as cursor:
        #     cursor.execute("SET FOREIGN_KEY_CHECKS = 0;")
        #     cursor.execute("DROP TABLE IF EXISTS projects, job_posting_tech_stack, job_posting, freelancer, employer, user")
        #     cursor.execute("SET FOREIGN_KEY_CHECKS = 1;")
        # cls.conn.commit()
        cls.conn.close()

    def setUp(self):
        # 각 테스트 시작 전 실행
        self.cursor = self.conn.cursor()
        
        # 1. 기존 데이터에 영향 없도록 트랜잭션 내에서 데이터 정리 (DELETE 사용)
        # 주의: TRUNCATE는 롤백이 안되므로 DELETE를 사용해야 함
        self.cursor.execute("SET FOREIGN_KEY_CHECKS = 0;")
        self.cursor.execute("DELETE FROM projects;")
        self.cursor.execute("DELETE FROM job_posting_tech_stack;")
        self.cursor.execute("DELETE FROM job_posting;")
        self.cursor.execute("DELETE FROM freelancer;")
        self.cursor.execute("DELETE FROM employer;")
        self.cursor.execute("DELETE FROM user;")
        self.cursor.execute("SET FOREIGN_KEY_CHECKS = 1;")

        # 2. 테스트 데이터 삽입 (insert_dummy_data.py 로직 활용)
        now = datetime.now()
        
        # 유저 생성
        sql_user = "INSERT INTO user (email, name, password, role, email_verified, privacy_agreed, terms_agreed, created_at, updated_at) VALUES (%s, %s, %s, %s, 1, 1, 1, %s, %s)"
        self.cursor.execute(sql_user, ('java_test@test.com', '테스트김철수', 'pw', 'FREELANCER', now, now))
        self.user_id = self.cursor.lastrowid
        
        self.cursor.execute(sql_user, ('corp_test@test.com', '테스트기업', 'pw', 'EMPLOYER', now, now))
        self.employer_user_id = self.cursor.lastrowid

        # 프리랜서/기업 프로필
        # DDL 기준: freelancer 테이블은 freelancer_id 직접 입력 필요, created_at -> created_date
        self.cursor.execute("INSERT INTO freelancer (freelancer_id, user_id, status, created_date, updated_at) VALUES (%s, %s, 'POTENTIAL', %s, %s)", (self.user_id, self.user_id, now, now))
        self.freelancer_id = self.user_id
        
        self.cursor.execute("INSERT INTO employer (user_id, company_name, scale, subscription, status, created_at, updated_at) VALUES (%s, '테스트기업', 'S100_299', 'PRO', 'ACTIVE', %s, %s)", (self.employer_user_id, now, now))
        self.employer_id = self.cursor.lastrowid

        # 공고 (Java 금융권)
        sql_job = "INSERT INTO job_posting (employer_id, employer_name, title, description, budget, duration, status, posting_status, headcount, matched_headcount, created_at, updated_at) VALUES (%s, %s, %s, %s, 8000000, 6, 'OPEN', 'OPEN', 1, 0, %s, %s)"
        self.cursor.execute(sql_job, (self.employer_id, '테스트기업', '테스트 금융권 차세대', '은행 계정계 Java 백엔드 개발', now, now))
        self.job_id = self.cursor.lastrowid

        # 기술 스택
        self.cursor.execute("INSERT INTO job_posting_tech_stack (job_posting_id, tech) VALUES (%s, 'Java')", (self.job_id,))
        self.cursor.execute("INSERT INTO job_posting_tech_stack (job_posting_id, tech) VALUES (%s, 'Spring Boot')", (self.job_id,))

        # 프로젝트 이력 (완료됨)
        sql_project = "INSERT INTO projects (employer_id, freelancer_id, job_posting_id, project_name, status, headcount, created_at, updated_at) VALUES (%s, %s, %s, '금융권 프로젝트', 'COMPLETED', 1, %s, %s)"
        self.cursor.execute(sql_project, (self.employer_id, self.freelancer_id, self.job_id, now, now))

    def tearDown(self):
        # 각 테스트 종료 후 실행
        # 1. DB 롤백 (INSERT 했던 데이터 모두 취소되고, DELETE 했던 기존 데이터 복구됨)
        self.conn.rollback()
        
        # 2. ChromaDB 테스트 컬렉션 삭제 (데이터 정리)
        try:
            self.vectorstore.delete_collection()
        except:
            pass

    def test_recommendation_logic(self):
        """
        통합 테스트:
        1. MariaDB에 있는 테스트 데이터를 ChromaDB로 동기화 (loader.py)
        2. RAG 체인을 통해 질문 (main.py 로직)
        3. 결과에 '테스트김철수'가 포함되는지 검증
        """
        print("\n🧪 [TEST] 데이터 동기화 및 RAG 추천 테스트 시작")
        
        # 1. 데이터 동기화 (현재 트랜잭션이 걸린 conn을 전달)
        # 주의: pymysql에서 같은 커넥션 내에서는 커밋하지 않아도 INSERT된 데이터를 읽을 수 있음
        sync_maria_to_chroma(conn=self.conn, vectorstore=self.vectorstore)
        
        # 2. RAG 체인 구성 (main.py의 로직과 동일하게 구성)
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

        retriever = self.vectorstore.as_retriever(search_kwargs={"k": 1})
        rag_chain = (
            {"context": itemgetter("input") | retriever | format_docs, "input": itemgetter("input")}
            | prompt
            | llm
            | StrOutputParser()
        )

        # 3. 질문 및 검증
        question = "금융권 Java 백엔드 개발자 추천해줘"
        print(f"❓ 질문: {question}")
        
        response = rag_chain.invoke({"input": question})
        answer = response # LCEL 체인은 문자열을 바로 반환합니다
        
        print(f"💡 답변: {answer}")

        # 검증: 답변에 우리가 넣은 '테스트김철수'가 언급되어야 함
        self.assertIn("테스트김철수", answer)
        print("✅ 검증 성공: '테스트김철수'가 추천되었습니다.")

if __name__ == '__main__':
    unittest.main()