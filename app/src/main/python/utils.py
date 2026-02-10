"""
Encryption utilities using AES-GCM with PBKDF2 key derivation
"""

import os
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
from cryptography.hazmat.backends import default_backend


def derive_key(passphrase: str, salt: bytes, iterations: int = 100000) -> bytes:
    """
    Derives a 256-bit AES key from passphrase using PBKDF2-SHA256.
    
    Args:
        passphrase: User-provided password
        salt: Random salt (16 bytes recommended)
        iterations: PBKDF2 iteration count
    
    Returns:
        32-byte AES key
    """
    kdf = PBKDF2HMAC(
        algorithm=hashes.SHA256(),
        length=32,
        salt=salt,
        iterations=iterations,
        backend=default_backend()
    )
    return kdf.derive(passphrase.encode('utf-8'))


def encrypt_message(plaintext: str, passphrase: str) -> tuple:
    """
    Encrypts plaintext using AES-GCM.
    
    Args:
        plaintext: Secret message to encrypt
        passphrase: Encryption password
    
    Returns:
        (salt, nonce, ciphertext) tuple
        - salt: 16 bytes
        - nonce: 12 bytes
        - ciphertext: encrypted bytes (includes auth tag)
    """
    # Generate random salt and nonce
    salt = os.urandom(16)
    nonce = os.urandom(12)
    
    # Derive encryption key
    key = derive_key(passphrase, salt)
    
    # Encrypt using AES-GCM
    aesgcm = AESGCM(key)
    ciphertext = aesgcm.encrypt(nonce, plaintext.encode('utf-8'), None)
    
    return salt, nonce, ciphertext