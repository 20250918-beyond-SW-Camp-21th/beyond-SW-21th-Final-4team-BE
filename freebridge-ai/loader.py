import os
from dotenv import load_dotenv
import pymysql
import pandas as pd
from database import get_vectorstore
from langchain_core.documents import Document

load_dotenv()

def sync_maria_to_chroma(conn=None, vectorstore=None):
    should_close = False
    # 외부에서 연결을 주지 않으면 새로 생성 (기존 로직 유지)
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
        # 공고(Job Posting)와 계약(Contract) 데이터를 합치는 쿼리
        query = """
        SELECT 
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

        for _, row in df.iterrows():
            # 검색의 정확도를 위해 '프로젝트 내용'을 가장 앞에 배치
            text = f"프로젝트 제목: {row['job_title']}\n" \
                   f"프로젝트 내용: {row['job_description']}\n" \
                   f"사용 기술: {row['job_requirement']}\n" \
                   f"수행 프리랜서: {row['freelancer_name']}"
            
            doc = Document(
                page_content=text,
                metadata={"freelancer_id": row['freelancer_id'], "type": "completed_project"}
            )
            documents.append(doc)

        if documents:
            vectorstore.add_documents(documents)
            print(f"Successfully synced {len(documents)} records.")

    finally:
        if should_close:
            conn.close()

if __name__ == "__main__":
    sync_maria_to_chroma()