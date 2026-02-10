"""
Main steganography encryption system
Uses corpus files from filipino_corpus folder
Defaults to trigram
"""

from collections import Counter, defaultdict
import os
import random
import re
from utils import encrypt_message as crypto_encrypt
from bitstream import create_payload
from zerowidth import embed_bits

MODE_LINGUISTIC = "linguistic"
MODE_ZEROWIDTH = "zerowidth"

DEFAULT_NGRAM_ORDER = 3
BASE_DIR = os.path.dirname(__file__)
CORPUS_FOLDER = os.path.join(BASE_DIR, "filipino_corpus")

class SimpleNgramModel:
    def __init__(self, ngram_order=3):
        self.ngram_order = ngram_order
        self.ngrams = defaultdict(list)
        self.ngram_counts = defaultdict(Counter)
        self.trained = False

    def tokenize(self, text):
        sentences = re.split(r'[.!?]+', text)
        all_tokens = []
        for sentence in sentences:
            sentence = sentence.strip()
            if not sentence:
                continue
            tokens = re.findall(r'\w+|[^\w\s]', sentence)
            if tokens and len(tokens) >= self.ngram_order:
                all_tokens.append(tokens)
        return all_tokens

    def train(self, text):
        sentence_list = self.tokenize(text)
        if not sentence_list:
            raise ValueError("No valid sentences in corpus")

        for tokens in sentence_list:
            marked = ["<START>"] * (self.ngram_order - 1) + tokens + ["<END>"]
            for i in range(len(marked) - self.ngram_order + 1):
                context = tuple(marked[i:i + self.ngram_order - 1])
                next_word = marked[i + self.ngram_order - 1]
                self.ngrams[context].append(next_word)
                self.ngram_counts[context][next_word] += 1

        self.trained = True

    def generate(self, bits=None, max_words=50, seed=None):
        if not self.trained:
            raise RuntimeError("Model not trained")
        random.seed(seed)
        context = ["<START>"] * (self.ngram_order - 1)
        sentence = []
        bit_index = 0
        for _ in range(max_words):
            current_context = tuple(context[-(self.ngram_order - 1):])
            next_word = self.pick_next_word(current_context)
            if not next_word or next_word == "<END>":
                break
            sentence.append(next_word)
            context.append(next_word)
        return " ".join(sentence)

    def pick_next_word(self, context):
        if context not in self.ngrams:
            return None
        return random.choice(self.ngrams[context])

def load_corpus_from_folder(corpus_folder=CORPUS_FOLDER):
    if not os.path.exists(corpus_folder):
        raise FileNotFoundError(f"Corpus folder not found: {corpus_folder}")

    txt_files = [f for f in os.listdir(corpus_folder) if f.endswith(".txt")]
    if not txt_files:
        raise FileNotFoundError(f"No .txt files found in: {corpus_folder}")

    corpus_text = []
    for txt_file in txt_files:
        with open(os.path.join(corpus_folder, txt_file), "r", encoding="utf-8") as f:
            corpus_text.append(f.read())

    return "\n".join(corpus_text)


def encrypt_message(
    secret: str,
    passphrase: str,
    corpus_text: str,
    mode: str = MODE_ZEROWIDTH,
    ngram_order: int = DEFAULT_NGRAM_ORDER,
    max_words: int = 50,
    seed: int = None
) -> str:

    if not secret:
        raise ValueError("Secret message cannot be empty")
    if not passphrase:
        raise ValueError("Passphrase cannot be empty")
    if not corpus_text or len(corpus_text.strip()) < 50:
        raise ValueError("Corpus text too short")

    salt, nonce, ciphertext = crypto_encrypt(secret, passphrase)
    bitstream = create_payload(salt, nonce, ciphertext)
    bits = [int(b) for b in bitstream]

    model = SimpleNgramModel(ngram_order=ngram_order)
    model.train(corpus_text)

    if mode == MODE_LINGUISTIC:
        return model.generate(bits=bits, max_words=max_words, seed=seed)

    if mode == MODE_ZEROWIDTH:
        cover = model.generate(bits=None, max_words=15, seed=seed)
        return embed_bits(cover, bitstream)

    raise ValueError(f"Unknown mode: {mode}")


def get_corpus_stats(corpus_text: str, ngram_order: int = DEFAULT_NGRAM_ORDER) -> dict:
    model = SimpleNgramModel(ngram_order=ngram_order)
    model.train(corpus_text)
    return model.get_stats()


def demo():
    print("=" * 70)
    print("FILIPINO LINGUISTIC STEGANOGRAPHY - ENCRYPTION DEMO")
    print("=" * 70)

    print("\n[LOADING CORPUS]")
    corpus_text = load_corpus_from_folder()
    print("Corpus loaded")
    print(f"Total length: {len(corpus_text)} characters")

    ngram_order = DEFAULT_NGRAM_ORDER

    print("\n[CORPUS ANALYSIS]")
    stats = get_corpus_stats(corpus_text, ngram_order)
    print("Corpus statistics:")
    print(f"  N-gram order: {stats['ngram_order']}")
    print(f"  Unique contexts: {stats['unique_contexts']}")
    print(f"  Vocabulary size: {stats['vocabulary_size']}")
    print(f"  Total unigrams: {stats['total_unigrams']}")


    print("\n" + "=" * 70)
    print("[ENCRYPTION INPUTS]")

    secret = input("Enter secret message: ").strip()
    passphrase = input("Enter passphrase: ").strip()

    print("\n[ENCRYPTING]")

    result = encrypt_message(
        secret=secret,
        passphrase=passphrase,
        corpus_text=corpus_text,
        mode=MODE_ZEROWIDTH,
        ngram_order=ngram_order,
        max_words=15,
        seed=None
    )

    visible = result.replace('\u200b', '').replace('\u200c', '')

    print("\n[STEGANOGRAPHIC OUTPUT]")
    print("Cover text (visible):")
    print(visible)

    print("\n[STATISTICS]")
    print(f"N-gram order: {ngram_order}")
    print(f"Visible length: {len(visible)} characters")
    print(f"Total length: {len(result)} characters")
    print(f"Hidden characters: {len(result) - len(visible)}")
    print(f"Word count: {len(visible.split())}")
    print("Encryption: AES-GCM")
    print("Embedding: Zero-width Unicode")

    from zerowidth import visualize_hidden
    print("\n[HIDDEN DATA PREVIEW]")
    print(visualize_hidden(result)[:150])

    print("\n" + "=" * 70)


if __name__ == "__main__":
    demo()
