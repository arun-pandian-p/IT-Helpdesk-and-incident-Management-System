import sys
import os

# Add backend to path
sys.path.insert(0, os.path.abspath(os.path.dirname(__file__)))

from app.db.database import SessionLocal
from app.services.seed_service import seed_database

if __name__ == "__main__":
    db = SessionLocal()
    try:
        print("Starting IT Helpdesk database seeding...")
        seed_database(db)
        print("Seeding finished successfully.")
    except Exception as e:
        print(f"Error seeding database: {e}")
        raise
    finally:
        db.close()
