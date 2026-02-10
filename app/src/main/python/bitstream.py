# bitstream.py

def bytes_to_bits(data: bytes) -> str:
    """
    Converts bytes to binary string.
    
    Args:
        data: Encrypted bytes
    
    Returns:
        Binary string (e.g., "10110101...")
    """
    return ''.join(format(byte, '08b') for byte in data)


def create_payload(salt: bytes, nonce: bytes, ciphertext: bytes) -> str:
    """
    Combines salt + nonce + ciphertext into single bitstream.
    
    Args:
        salt: 16 bytes
        nonce: 12 bytes
        ciphertext: Variable length encrypted data
    
    Returns:
        Binary string representing complete payload
    """
    full_payload = salt + nonce + ciphertext
    return bytes_to_bits(full_payload)


def text_to_bits(text: str) -> list:
    """
    Converts text to list of bits (for compatibility with original code).
    
    Args:
        text: Input text
    
    Returns:
        List of integers (0 or 1)
    """
    bits = []
    for c in text:
        bits.extend([int(b) for b in format(ord(c), "08b")])
    return bits