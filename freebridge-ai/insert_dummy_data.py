import os
from dotenv import load_dotenv
import pymysql
from datetime import datetime

load_dotenv()

# DB 연결 설정
try:
    conn = pymysql.connect(
        host=os.getenv("DB_HOST", "localhost"),
        user=os.getenv("DB_USER", "root"),
        password=os.getenv("DB_PASSWORD", "password"),
        db=os.getenv("DB_NAME", "freebridge"),
        port=int(os.getenv("DB_PORT", 3306)),
        charset='utf8mb4',
        autocommit=True
    )
except pymysql.Error as e:
    print(f"❌ DB 연결 실패: {e}")
    exit()

def insert_dummy_data():
    now = datetime.now()
    try:
        with conn.cursor() as cursor:
            print("🔄 기존 테스트 데이터 정리 중...")
            # 외래 키 제약 조건을 고려하여 자식 테이블부터 삭제
            cursor.execute("SET FOREIGN_KEY_CHECKS = 0;")
            cursor.execute("TRUNCATE TABLE projects;")
            cursor.execute("TRUNCATE TABLE job_posting_tech_stack;")
            cursor.execute("TRUNCATE TABLE job_posting;")
            cursor.execute("TRUNCATE TABLE freelancer;")
            cursor.execute("TRUNCATE TABLE employer;")
            cursor.execute("TRUNCATE TABLE user;")
            cursor.execute("SET FOREIGN_KEY_CHECKS = 1;")

            print("1️⃣ 사용자(user) 데이터 생성 중...")
            sql_user = """
            INSERT INTO user (email, name, password, role, email_verified, privacy_agreed, terms_agreed, created_at, updated_at)
            VALUES (%s, %s, %s, %s, 1, 1, 1, %s, %s)
            """
            # 프리랜서 유저
            cursor.execute(sql_user, ('java_expert@test.com', '김철수', 'password', 'FREELANCER', now, now))
            user_chulsoo_id = cursor.lastrowid
            
            cursor.execute(sql_user, ('ai_expert@test.com', '이영희', 'password', 'FREELANCER', now, now))
            user_younghee_id = cursor.lastrowid

            # 기업 유저
            cursor.execute(sql_user, ('techcorp@test.com', 'TechCorp', 'password', 'EMPLOYER', now, now))
            user_employer_id = cursor.lastrowid

            print("2️⃣ 프리랜서(freelancer) 및 기업(employer) 프로필 생성 중...")
            # DDL 기준: freelancer 테이블은 freelancer_id 직접 입력 필요, created_at -> created_date
            cursor.execute("INSERT INTO freelancer (freelancer_id, user_id, status, created_date, updated_at) VALUES (%s, %s, 'POTENTIAL', %s, %s)", (user_chulsoo_id, user_chulsoo_id, now, now))
            freelancer_chulsoo_id = user_chulsoo_id
            
            cursor.execute("INSERT INTO freelancer (freelancer_id, user_id, status, created_date, updated_at) VALUES (%s, %s, 'POTENTIAL', %s, %s)", (user_younghee_id, user_younghee_id, now, now))
            freelancer_younghee_id = user_younghee_id

            sql_employer = """
            INSERT INTO employer (user_id, company_name, scale, subscription, status, created_at, updated_at)
            VALUES (%s, %s, 'S100_299', 'PRO', 'ACTIVE', %s, %s)
            """
            cursor.execute(sql_employer, (user_employer_id, '테크코프', now, now))
            employer_id = cursor.lastrowid

            print("3️⃣ 채용 공고(job_posting) 데이터 생성 중...")
            sql_job = """
            INSERT INTO job_posting (employer_id, employer_name, title, description, budget, duration, status, posting_status, headcount, matched_headcount, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s, 'OPEN', 'OPEN', %s, 0, %s, %s)
            """
            # 공고 1: 금융권 차세대 (Java)
            cursor.execute(sql_job, (employer_id, '테크코프', '금융권 차세대 시스템 구축', '은행 계정계 시스템의 백엔드 모듈 개발 및 성능 최적화 업무. 대용량 트래픽 처리가 중요함.', 8000000, 6, 2, now, now))
            job_bank_id = cursor.lastrowid
            
            # 공고 2: AI 챗봇 (Python)
            cursor.execute(sql_job, (employer_id, '테크코프', 'AI 스타트업 챗봇 서비스 개발', 'LLM 기반의 챗봇 서비스 백엔드 및 프롬프트 엔지니어링. RAG 시스템 구축 경험 필요.', 10000000, 4, 1, now, now))
            job_ai_id = cursor.lastrowid

            print("4️⃣ 공고 기술 스택(job_posting_tech_stack) 데이터 생성 중...")
            sql_tech = "INSERT INTO job_posting_tech_stack (job_posting_id, tech) VALUES (%s, %s)"
            cursor.execute(sql_tech, (job_bank_id, 'Java'))
            cursor.execute(sql_tech, (job_bank_id, 'Spring Boot'))
            cursor.execute(sql_tech, (job_bank_id, 'Oracle'))
            
            cursor.execute(sql_tech, (job_ai_id, 'Python'))
            cursor.execute(sql_tech, (job_ai_id, 'FastAPI'))
            cursor.execute(sql_tech, (job_ai_id, 'LangChain'))

            print("5️⃣ 프로젝트 수행 이력(projects) 데이터 생성 중...")
            sql_project = """
            INSERT INTO projects (employer_id, freelancer_id, job_posting_id, project_name, status, headcount, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, 1, %s, %s)
            """
            # 김철수 - 금융권 프로젝트 (완료 -> RAG 검색 대상)
            cursor.execute(sql_project, (employer_id, freelancer_chulsoo_id, job_bank_id, '금융권 차세대', 'COMPLETED', now, now))
            
            # 이영희 - AI 프로젝트 (완료 -> RAG 검색 대상)
            cursor.execute(sql_project, (employer_id, freelancer_younghee_id, job_ai_id, 'AI 챗봇 개발', 'COMPLETED', now, now))

            print("✅ 테스트 데이터 입력이 완료되었습니다!")

    except Exception as e:
        print(f"❌ 데이터 입력 중 오류 발생: {e}")
    finally:
        conn.close()

if __name__ == "__main__":
    insert_dummy_data()