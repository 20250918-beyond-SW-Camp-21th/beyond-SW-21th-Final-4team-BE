import os
import sys
import pymysql
from dotenv import load_dotenv
from datetime import datetime

load_dotenv()

def get_db_connection():
    """DB 연결 객체를 생성하여 반환합니다."""
    try:
        return pymysql.connect(
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
        raise

def insert_dummy_data():
    db_name = os.getenv("DB_NAME", "freebridge")
    db_host = os.getenv("DB_HOST", "localhost")
    allow_destructive = os.getenv("ALLOW_DESTRUCTIVE", "false").lower() == "true"
    
    # [개선] 안전장치: DB 이름에 'test'가 없거나 로컬이 아닌 경우 명시적 허용 확인
    is_safe_db = "test" in db_name.lower() or db_name.lower().endswith("_dev") or db_host in ["localhost", "127.0.0.1"]
    
    if not is_safe_db and not allow_destructive:
        print(f"❌ 보호: 운영/공용 DB({db_name})에서 실행이 차단되었습니다.")
        print("정말 실행하려면 환경변수에 ALLOW_DESTRUCTIVE=true를 설정하세요.")
        sys.exit(1)

    conn = get_db_connection()
    now = datetime.now()
    try:
        with conn.cursor() as cursor:
            # [개선] 기존 데이터를 전체 삭제(TRUNCATE)하지 않고 진행하거나, 
            # 필요 시 재우가 넣었던 테스트 계정만 삭제하는 로직을 권장함.
            print(f"🔄 {db_name}에 테스트 데이터 삽입 시작...")

            sql_user = """
            INSERT INTO user (email, name, password, role, email_verified, privacy_agreed, terms_agreed, created_at, updated_at)
            VALUES (%s, %s, %s, %s, 1, 1, 1, %s, %s)
            """
            
            # 테스트 데이터 생성
            cursor.execute(sql_user, ('java_expert@test.com', '김철수', 'password', 'FREELANCER', now, now))
            user_chulsoo_id = cursor.lastrowid
            
            cursor.execute(sql_user, ('ai_expert@test.com', '이영희', 'password', 'FREELANCER', now, now))
            user_younghee_id = cursor.lastrowid

            cursor.execute(sql_user, ('techcorp@test.com', 'TechCorp', 'password', 'EMPLOYER', now, now))
            user_employer_id = cursor.lastrowid

            # 프로필 생성
            cursor.execute("INSERT INTO freelancer (freelancer_id, user_id, status, created_date, updated_at) VALUES (%s, %s, 'POTENTIAL', %s, %s)", (user_chulsoo_id, user_chulsoo_id, now, now))
            cursor.execute("INSERT INTO employer (user_id, company_name, scale, subscription, status, created_at, updated_at) VALUES (%s, '테크코프', 'S100_299', 'PRO', 'ACTIVE', %s, %s)", (user_employer_id, now, now))
            employer_id = cursor.lastrowid

            # 공고 생성
            sql_job = """
            INSERT INTO job_posting (employer_id, employer_name, title, description, budget, duration, status, posting_status, headcount, matched_headcount, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s, 'OPEN', 'OPEN', %s, 0, %s, %s)
            """
            cursor.execute(sql_job, (employer_id, '테크코프', '금융 시스템 구축', 'Java 백엔드 개발', 8000000, 6, 2, now, now))
            job_bank_id = cursor.lastrowid

            # 기술 스택
            sql_tech = "INSERT INTO job_posting_tech_stack (job_posting_id, tech) VALUES (%s, %s)"
            cursor.execute(sql_tech, (job_bank_id, 'Java'))
            cursor.execute(sql_tech, (job_bank_id, 'Spring Boot'))

            # 프로젝트 이력
            sql_project = """
            INSERT INTO projects (employer_id, freelancer_id, job_posting_id, project_name, status, headcount, created_at, updated_at)
            VALUES (%s, %s, %s, %s, 'COMPLETED', 1, %s, %s)
            """
            cursor.execute(sql_project, (employer_id, user_chulsoo_id, job_bank_id, '금융권 차세대', now, now))

            print("✅ 테스트 데이터 입력이 완료되었습니다!")
    except Exception as e:
        print(f"❌ 오류 발생: {e}")
        raise # [개선] 에러를 삼키지 않고 상위로 전달
    finally:
        conn.close()

if __name__ == "__main__":
    insert_dummy_data()