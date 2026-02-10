"""
Automatic corpus preprocessing for Filipino text
Processes all .txt files in filipino_corpus
Creates both trigram and quadgram versions automatically
"""

import re
import os

BASE_DIR = os.path.dirname(__file__)
CORPUS_FOLDER = os.path.join(BASE_DIR, "filipino_corpus")
OUTPUT_FOLDER = os.path.join(BASE_DIR, "processed_corpus")


def tokenize_sentence(text):
    """
    Tokenize text into sentences and words.
    
    Args:
        text: Raw input text
    
    Returns:
        List of token lists (one list per sentence)
    """
    # Split on sentence-ending punctuation
    sentences = re.split(r'[.!?]+', text)
    
    all_tokens = []
    for sentence in sentences:
        sentence = sentence.strip()
        if not sentence:
            continue
        
        # Extract words and punctuation
        tokens = re.findall(r'\w+|[^\w\s]', sentence)
        if tokens:
            all_tokens.append(tokens)
    
    return all_tokens


def add_line_marker(tokens, ngram_order):
    """
    Add start and end markers to token list.
    
    Args:
        tokens: List of word tokens
        ngram_order: N-gram order (3 or 4)
    
    Returns:
        List with markers added
    """
    return ["<START>"] * (ngram_order - 1) + tokens + ["<END>"]


def process_file(path, output_folder, ngram_order):
    """
    Process a single corpus file for specific n-gram order.
    
    Args:
        path: Path to input corpus file
        output_folder: Output directory
        ngram_order: N-gram order 3 and 4
    
    Returns:
        Number of sentences processed
    """
    with open(path, "r", encoding="utf-8") as f:
        text = f.read()

    # Tokenize
    sentence_tokens = tokenize_sentence(text)
    
    # Process sentences
    processed_sentences = []
    for tokens in sentence_tokens:
        if len(tokens) >= ngram_order:
            processed_sentences.append(add_line_marker(tokens, ngram_order))
    
    if not processed_sentences:
        return 0

    # Create output directory
    os.makedirs(output_folder, exist_ok=True)
    
    # Output filename
    output_path = os.path.join(output_folder, os.path.basename(path))

    # Write processed sentences
    with open(output_path, "w", encoding="utf-8") as f:
        for sent_tokens in processed_sentences:
            f.write(" ".join(sent_tokens) + "\n")

    return len(processed_sentences)


def process_all_files():
    """
    Process all .txt files in corpus folder.
    Creates both trigram and quadgram versions automatically.
    """
    print("=" * 70)
    print("FILIPINO CORPUS PREPROCESSOR")
    print("=" * 70)
    
    # Check if corpus folder exists
    if not os.path.exists(CORPUS_FOLDER):
        print(f"\nCorpus folder not found: {CORPUS_FOLDER}")
        print(f"\nPlease create the folder and add .txt files:")
        print(f"  mkdir {CORPUS_FOLDER}")
        print(f"  # Add your Filipino text files (.txt) to this folder")
        return
    
    # Get all .txt files
    txt_files = [f for f in os.listdir(CORPUS_FOLDER) if f.endswith(".txt")]
    
    if not txt_files:
        print(f"\nNo .txt files found in: {CORPUS_FOLDER}")
        print(f"\nPlease add Filipino corpus text files (.txt) to this folder.")
        return
    
    print(f"\nFound {len(txt_files)} corpus file(s)")
    print(f"\nFiles:")
    for f in txt_files:
        print(f"  - {f}")
    
    # Process for both trigram and quadgram
    for ngram_order in [3, 4]:
        print(f"\n{'=' * 70}")
        print(f"Processing for {ngram_order}-gram model...")
        print(f"{'=' * 70}")
        
        # Create output folder for this n-gram order
        output_folder = os.path.join(OUTPUT_FOLDER, f"{ngram_order}gram")
        
        total_sentences = 0
        
        for txt_file in txt_files:
            file_path = os.path.join(CORPUS_FOLDER, txt_file)
            
            # Process file
            count = process_file(file_path, output_folder, ngram_order)
            total_sentences += count
            
            print(f"  {txt_file}: {count} sentences")
        
        print(f"\n  Total sentences for {ngram_order}-gram: {total_sentences}")
        print(f"  Output folder: {output_folder}")
    
    print(f"\n{'=' * 70}")
    print("CORPUS PREPROCESSING COMPLETE")
    print(f"{'=' * 70}")
    print(f"\nOutput structure:")
    print(f"  {OUTPUT_FOLDER}/")
    print(f"    ├── 3gram/  (trigram processed files)")
    print(f"    └── 4gram/  (quadgram processed files)")
    print(f"\nNext step: Run build_database.py to create n-gram databases")


if __name__ == "__main__":
    process_all_files()