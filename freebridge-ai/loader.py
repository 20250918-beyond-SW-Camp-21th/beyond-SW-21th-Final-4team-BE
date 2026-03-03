# loader.py
import os
import pandas as pd
import pymysql
from dotenv import load_dotenv
from database import get_vectorstore
from langchain_core.documents import Document

load_dotenv()

def sync_maria_to_chroma():
    conn = pymysql.connect(
        host=os.getenv("DB_HOST", "localhost"),
        user=os.getenv("DB_USER", "root"),
        password=os.getenv("DB_PASSWORD", "password"),
        db=os.getenv("DB_NAME", "freebridge"),
        port=int(os.getenv("DB_PORT", 3306)),
        charset='utf8mb4',
        cursorclass=pymysql.cursors.DictCursor
    )

    try:
        vectorstore = get_vectorstore() # 기본 컬렉션 사용
        all_documents = []
        all_ids = []

        # --- Part 1: 완료된 프로젝트 경험 (기업이 프리랜서 찾을 때 사용) ---
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
            
            # [수정] status 메타데이터 추가 (CONTRACTING 필터링용)
            all_documents.append(Document(
                page_content=text, 
                metadata={
                    "id": row['id'], 
                    "type": "experience", 
                    "ref_id": row['freelancer_id'],
                    "status": row['freelancer_status'] # DB의 실제 프리랜서 상태값 저장
                }
            ))
            all_ids.append(f"exp:{row['id']}")

        # --- Part 2: 활성화된 채용 공고 (프리랜서가 공고 찾을 때 사용) ---
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
            
            # [수정] 채용공고에도 기본 status 'ACTIVE' 추가 (에러 방지용)
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
            # 동일한 ID가 있으면 덮어쓰기(Upsert) 방식으로 작동함
            vectorstore.add_documents(all_documents, ids=all_ids)
            print(f"동기화 완료! 프로젝트: {len(df_projects)}건, 채용공고: {len(df_jobs)}건")

    finally:
        conn.close()

if __name__ == "__main__":
    sync_maria_to_chroma()