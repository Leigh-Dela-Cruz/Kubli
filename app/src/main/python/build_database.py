"""
Builds n-gram databases from processed corpus.
Creates both trigram and quadgram databases automatically.
No user input required - just run it.
"""

import sqlite3
from collections import Counter
import re
import os

BASE_DIR = os.path.dirname(__file__)
CORPUS_FOLDER = os.path.join(BASE_DIR, "processed_corpus")
DATABASE_FOLDER = os.path.join(BASE_DIR, "ngram_database")


def tokenize(line):
    """
    Tokenize a line preserving START/END markers.
    
    Args:
        line: Input line
    
    Returns:
        List of tokens
    """
    return re.findall(r'<START>|<END>|\w+|[^\w\s]', line)


def build_database(ngram_order):
    """
    Build database for specific n-gram order.
    
    Args:
        ngram_order: 3 for trigram, 4 for quadgram
    
    Returns:
        Number of n-grams created
    """
    # Input folder for this n-gram order
    input_folder = os.path.join(CORPUS_FOLDER, f"{ngram_order}gram")
    
    if not os.path.exists(input_folder):
        print(f"  Folder not found: {input_folder}")
        print(f"     Run process_corpus.py first")
        return 0
    
    # Count n-grams
    ngrams = Counter()
    file_count = 0
    
    txt_files = [f for f in os.listdir(input_folder) if f.endswith(".txt")]
    
    if not txt_files:
        print(f"  No processed files found in: {input_folder}")
        return 0
    
    # Read all processed files
    for txt_file in txt_files:
        file_count += 1
        path = os.path.join(input_folder, txt_file)
        
        with open(path, "r", encoding="utf-8") as f:
            for line in f:
                tokens = tokenize(line.strip())
                
                # Build n-grams
                for i in range(len(tokens) - ngram_order + 1):
                    ngram = tuple(tokens[i:i + ngram_order])
                    ngrams[ngram] += 1
    
    if not ngrams:
        print(f"  No n-grams found")
        return 0
    
    # Create database directory
    os.makedirs(DATABASE_FOLDER, exist_ok=True)
    
    # Database filename
    db_name = f"filipino_{ngram_order}gram.db"
    db_path = os.path.join(DATABASE_FOLDER, db_name)
    
    # Create SQLite database
    connection = sqlite3.connect(db_path)
    c = connection.cursor()
    
    # Drop existing table
    c.execute("DROP TABLE IF EXISTS ngrams")
    
    # Create table based on n-gram order
    if ngram_order == 3:
        c.execute("""
        CREATE TABLE ngrams (
            word1 TEXT,
            word2 TEXT,
            word3 TEXT,
            frequency INTEGER,
            PRIMARY KEY (word1, word2, word3)
        )
        """)
        
        c.execute("CREATE INDEX idx_context ON ngrams (word1, word2)")
        
        # Insert trigrams
        c.executemany(
            "INSERT INTO ngrams VALUES (?, ?, ?, ?)",
            [(w1, w2, w3, freq) for (w1, w2, w3), freq in ngrams.items()]
        )
        
    elif ngram_order == 4:
        c.execute("""
        CREATE TABLE ngrams (
            word1 TEXT,
            word2 TEXT,
            word3 TEXT,
            word4 TEXT,
            frequency INTEGER,
            PRIMARY KEY (word1, word2, word3, word4)
        )
        """)
        
        c.execute("CREATE INDEX idx_context ON ngrams (word1, word2, word3)")
        
        # Insert quadgrams
        c.executemany(
            "INSERT INTO ngrams VALUES (?, ?, ?, ?, ?)",
            [(w1, w2, w3, w4, freq) for (w1, w2, w3, w4), freq in ngrams.items()]
        )
    
    connection.commit()
    connection.close()
    
    print(f"  ✓ Database created: {db_name}")
    print(f"    Files processed: {file_count}")
    print(f"    Total {ngram_order}-grams: {len(ngrams):,}")
    
    return len(ngrams)


def build_all_databases():
    """
    Build databases for both trigram and quadgram.
    """
    print("=" * 70)
    print("FILIPINO N-GRAM DATABASE BUILDER")
    print("=" * 70)
    
    # Check if processed corpus exists
    if not os.path.exists(CORPUS_FOLDER):
        print(f"\nProcessed corpus folder not found: {CORPUS_FOLDER}")
        print(f"\nPlease run process_corpus.py first")
        return
    
    print(f"\nBuilding databases from: {CORPUS_FOLDER}\n")
    
    # Build both databases
    total_stats = {}
    
    for ngram_order in [3, 4]:
        print(f"{'=' * 70}")
        print(f"Building {ngram_order}-gram database...")
        print(f"{'=' * 70}")
        
        count = build_database(ngram_order)
        total_stats[ngram_order] = count
        print()
    
    # Summary
    print(f"{'=' * 70}")
    print("DATABASE CREATION COMPLETE")
    print(f"{'=' * 70}")
    print(f"\nDatabases created:")
    print(f"  Location: {DATABASE_FOLDER}/")
    
    if total_stats.get(3, 0) > 0:
        print(f"  filipino_3gram.db ({total_stats[3]:,} trigrams)")
    
    if total_stats.get(4, 0) > 0:
        print(f"  filipino_4gram.db ({total_stats[4]:,} quadgrams)")
    
    if not any(total_stats.values()):
        print(f"  No databases created")
        print(f"\nPlease check:")
        print(f"  1. Corpus files exist in: {CORPUS_FOLDER}")
        print(f"  2. Files are properly formatted")
    else:
        print(f"\n✓ Ready for encryption!")


if __name__ == "__main__":
    build_all_databases()