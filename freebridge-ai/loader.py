# loader.py
import os
import pandas as pd
import pymysql
from dotenv import load_dotenv
from database import get_vectorstore
from langchain_core.documents import Document

load_dotenv()

def sync_maria_to_chroma():
    db_user = os.getenv("DB_USER")
    db_password = os.getenv("DB_PASSWORD")
    db_name = os.getenv("DB_NAME")
    db_host = os.getenv("DB_HOST", "localhost")
    db_port = int(os.getenv("DB_PORT", 3306))

    if not all([db_user, db_password, db_name]):
        raise RuntimeError(
            f"필수 DB 환경변수가 누락되었습니다: "
            f"DB_USER={db_user}, DB_PASSWORD={'***' if db_password else None}, DB_NAME={db_name}"
        )

    conn = pymysql.connect(
        host=db_host,
        user=db_user,
        password=db_password,
        db=db_name,
        port=db_port,
        charset='utf8mb4',
        cursorclass=pymysql.cursors.DictCursor
    )

    try:
        vectorstore = get_vectorstore() 
        all_documents = []
        all_ids = []

        # --- Part 1: 완료된 프로젝트 경험 ---
        project_query = """
        SELECT p.id, p.freelancer_id, u.name, j.title, j.description, f.status as freelancer_status,
               (SELECT GROUP_CONCAT(tech SEPARATOR ', ') FROM job_posting_tech_stack jts WHERE jts.job_posting_id = j.id) as techs
        FROM projects p
        JOIN freelancer f ON p.freelancer_id = f.freelancer_id
        JOIN user u ON f.user_id = u.user_id
        JOIN job_posting j ON p.job_posting_id = j.id
        WHERE p.status = 'COMPLETED'
        """
        df_projects = pd.read_sql(project_query, conn)
        for _, row in df_projects.iterrows():
            techs = str(row['techs']) if pd.notna(row['techs']) else "없음"
            text = f"경험 프로젝트: {row['title']}\n기술: {techs}\n설명: {row['description']}\n담당: {row['name']}"
            
            all_documents.append(Document(
                page_content=text, 
                metadata={
                    "id": row['id'], 
                    "type": "experience", 
                    "ref_id": row['freelancer_id'],
                    "status": row['freelancer_status'] 
                }
            ))
            all_ids.append(f"exp:{row['id']}")

        # --- Part 2: 활성화된 채용 공고 ---
        job_query = """
        SELECT j.id, j.title, j.description, j.budget,
               (SELECT GROUP_CONCAT(tech SEPARATOR ', ') FROM job_posting_tech_stack jts WHERE jts.job_posting_id = j.id) as techs
        FROM job_posting j
        WHERE j.status = 'ACTIVE'
        """
        df_jobs = pd.read_sql(job_query, conn)
        for _, row in df_jobs.iterrows():
            techs = str(row['techs']) if pd.notna(row['techs']) else "없음"
            text = f"채용공고: {row['title']}\n요구기술: {techs}\n상세내용: {row['description']}\n예산: {row['budget']}"
            
            all_documents.append(Document(
                page_content=text, 
                metadata={
                    "id": row['id'], 
                    "type": "job_posting", 
                    "ref_id": row['id'],
                    "status": "ACTIVE" 
                }
            ))
            all_ids.append(f"job:{row['id']}")

        # 벡터 DB 업데이트
        if all_documents:
            vectorstore.add_documents(all_documents, ids=all_ids)
            print(f"동기화 완료! 프로젝트: {len(df_projects)}건, 채용공고: {len(df_jobs)}건")

    finally:
        conn.close()

if __name__ == "__main__":
    sync_maria_to_chroma()