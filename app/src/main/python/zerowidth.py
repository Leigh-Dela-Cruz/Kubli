# zerowidth.py

ZERO = '\u200b'  # Zero-width space (bit 0)
ONE = '\u200c'   # Zero-width non-joiner (bit 1)


def embed_bits(cover_text: str, bitstream: str) -> str:
    """
    Embeds bitstream into cover text using zero-width characters.
    
    Args:
        cover_text: Normal readable text
        bitstream: Binary string or list of bits
    
    Returns:
        Steganographic text with hidden data
    """
    # Convert list to string if needed
    if isinstance(bitstream, list):
        bitstream = ''.join(str(b) for b in bitstream)
    
    result = []
    bit_index = 0
    
    for char in cover_text:
        result.append(char)
        
        # Inject after spaces or punctuation
        if char in (' ', '.', ',', '!', '?', ';', ':'):
            if bit_index < len(bitstream):
                bit = bitstream[bit_index]
                result.append(ONE if bit == '1' else ZERO)
                bit_index += 1
    
    # Append remaining bits at end
    while bit_index < len(bitstream):
        bit = bitstream[bit_index]
        result.append(ONE if bit == '1' else ZERO)
        bit_index += 1
    
    return ''.join(result)


def visualize_hidden(stego_text: str) -> str:
    """
    Debug helper: replaces zero-width chars with visible symbols.
    """
    return stego_text.replace(ZERO, '[0]').replace(ONE, '[1]')