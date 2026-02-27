import os
import pandas as pd
import pymysql
from dotenv import load_dotenv
from database import get_vectorstore
from langchain_core.documents import Document

load_dotenv()

def sync_maria_to_chroma(conn=None, vectorstore=None):
    should_close = False
    if conn is None:
        conn = pymysql.connect(
            host=os.getenv("DB_HOST", "localhost"),
            user=os.getenv("DB_USER", "root"),
            password=os.getenv("DB_PASSWORD", "password"),
            db=os.getenv("DB_NAME", "freebridge"),
            port=int(os.getenv("DB_PORT", 3306)),
            charset='utf8mb4',
            cursorclass=pymysql.cursors.DictCursor
        )
        should_close = True

    try:
        # 중복 방지를 위해 p.id를 추가로 조회
        query = """
        SELECT 
            p.id as project_id,
            p.freelancer_id,
            u.name as freelancer_name,
            j.title as job_title,
            j.description as job_description,
            (SELECT GROUP_CONCAT(tech SEPARATOR ', ') 
             FROM job_posting_tech_stack jts 
             WHERE jts.job_posting_id = j.id) as job_requirement
        FROM projects p
        JOIN freelancer f ON p.freelancer_id = f.freelancer_id
        JOIN user u ON f.user_id = u.user_id
        JOIN job_posting j ON p.job_posting_id = j.id
        WHERE p.status = 'COMPLETED'
        """
        df = pd.read_sql(query, conn)
        
        if vectorstore is None:
            vectorstore = get_vectorstore()
            
        documents = []
        doc_ids = []

        for _, row in df.iterrows():
            # [개선] Null/None 처리: 기술 스택이 없으면 "없음"으로 대체
            requirement = row['job_requirement'] if row['job_requirement'] else "없음"
            
            text = f"프로젝트 제목: {row['job_title']}\n" \
                   f"프로젝트 내용: {row['job_description']}\n" \
                   f"사용 기술: {requirement}\n" \
                   f"수행 프리랜서: {row['freelancer_name']}"
            
            doc = Document(
                page_content=text,
                metadata={"freelancer_id": row['freelancer_id'], "type": "completed_project"}
            )
            documents.append(doc)
            # [개선] 중복 방지를 위해 고유 ID (project:ID) 사용
            doc_ids.append(f"project:{row['project_id']}")

        if documents:
            # ids를 전달하여 같은 프로젝트가 다시 들어와도 덮어쓰도록 함
            vectorstore.add_documents(documents, ids=doc_ids)
            print(f"Successfully synced {len(documents)} records.")

    finally:
        if should_close:
            conn.close()

if __name__ == "__main__":
    sync_maria_to_chroma()