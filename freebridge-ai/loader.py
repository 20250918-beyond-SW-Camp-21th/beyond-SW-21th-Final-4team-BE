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
        # 중복 방지를 위해 p.id를 명시적으로 가져옴
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
            # [수정] pandas NaN 처리: 데이터가 없으면 "없음"으로 표시
            raw_req = row['job_requirement']
            requirement = str(raw_req).strip() if pd.notna(raw_req) and raw_req else "없음"
            
            text = f"프로젝트 제목: {row['job_title']}\n" \
                   f"프로젝트 내용: {row['job_description']}\n" \
                   f"사용 기술: {requirement}\n" \
                   f"수행 프리랜서: {row['freelancer_name']}"
            
            doc = Document(
                page_content=text,
                metadata={"freelancer_id": row['freelancer_id'], "type": "completed_project"}
            )
            documents.append(doc)
            # [수정] 고유 ID 부여로 여러 번 실행해도 중복 생성 방지
            doc_ids.append(f"project:{row['project_id']}")

        if documents:
            # ids 인자를 사용하여 기존 데이터를 업데이트(Upsert) 함
            vectorstore.add_documents(documents, ids=doc_ids)
            print(f"Successfully synced {len(documents)} records.")

    finally:
        if should_close:
            conn.close()

if __name__ == "__main__":
    sync_maria_to_chroma()