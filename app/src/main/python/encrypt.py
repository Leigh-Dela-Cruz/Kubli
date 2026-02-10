# encrypt.py
import os
import random

BASE_DIR = os.path.dirname(__file__)
CORPUS_FOLDER = os.path.join(BASE_DIR, "filipino_corpus")

def load_corpus():
    """Load all .txt files in the corpus folder and combine them."""
    texts = []
    try:
        files = [f for f in os.listdir(CORPUS_FOLDER) if f.endswith(".txt")]
        for f in files:
            with open(os.path.join(CORPUS_FOLDER, f), "r", encoding="utf-8") as file:
                texts.append(file.read())
    except Exception as e:
        return f"Error loading corpus: {e}"
    return "\n".join(texts)

def generate_text(input_length=20):
    """Return a coherent snippet of text roughly proportional to input_length."""
    corpus = load_corpus()
    if not corpus.strip():
        return "Corpus is empty or missing"

    # Split corpus into sentences
    sentences = [s.strip() for s in corpus.replace("\n", " ").split(".") if s.strip()]
    if not sentences:
        return "Corpus has no usable sentences"

    # Randomly pick sentences until roughly input_length words
    output_words = []
    while len(output_words) < input_length:
        s = random.choice(sentences)
        output_words.extend(s.split())
        if len(output_words) > input_length * 1.5:  # limit to avoid too long
            break

    # Trim to reasonable length
    return " ".join(output_words[:int(input_length*1.2)]).strip() + "."

def hide_message_safe(secret=""):
    """Dummy wrapper for Kotlin; ignores secret but can take its length."""
    input_len = len(secret.split()) if secret else 20
    return generate_text(input_len)