#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "stmobile_crypto.h"
#include "stmobile_getscript.h"

#define STGL_STARTCODE_LEN	(4)
#define STGL_PADDING_LEN	(8)
#define STGL_TIME_LEN		(16)
const char stgl_startcode[] = "STGL";

#define STGL_HEADER_LEN		(STGL_STARTCODE_LEN + STGL_PADDING_LEN + sizeof(int32_t))

// read encrypt key & iv
static inline int read_aes_key(AES_CTX *aes_ctx) {
#define SYMM_SWAP(ch)                                   \
	(((ch & 1) << 7) | ((ch & (2)) << 5) | ((ch & (4)) << 3) |  \
	 ((ch & (8)) << 1) | ((ch & (16)) >> 1) | ((ch & (32)) >> 3) | \
	 ((ch & (64)) >> 5) | ((ch & (128)) >> 7))

	// set key
	const unsigned char enkey[32] = {
		0x97, 0xd9, 0x31, 0xef, 0xd7, 0x89, 0xf7, 0x51,
		0x89, 0x57, 0xb7, 0x81, 0x89, 0xf9, 0xd9, 0xd9,
		0x01, 0x79, 0xd7, 0x17, 0xf1, 0xf9, 0x57, 0x11,
		0x37, 0xd7, 0x29, 0x39, 0x9, 0xc1, 0x89, 0x41
	};
	int key_len = strlen((const char *)enkey);
	uint8_t *key = (uint8_t *)calloc(1, key_len + 1);
	memcpy(key, enkey, key_len);
	for (int i = 0; i < 32; i++) {
		key[i] = key[i] ^ 0x95;
		key[i] = SYMM_SWAP(key[i]);
	}
#undef SYMM_SWAP
	uint8_t iv[] = "F43481FC1ED8FFB27F2C7A52B7FF0D73";
	AES_set_key(aes_ctx, key, iv, AES_MODE_128);
	free(key);
	return 0;
}

int stgl_decrypt2mem(unsigned char *start, unsigned char *fill,
		     unsigned char **output_buf) {
	if (!start || !fill) {
		return -1;
	} else {
		unsigned char *pos = start;
		if (strncmp((const char*)pos, stgl_startcode, STGL_STARTCODE_LEN) != 0) {
			printf("not encrypt memory\n");
			return -1;
		}

		int len = 0;
		memcpy(&len, pos + STGL_STARTCODE_LEN, sizeof(int32_t));

		AES_CTX ctx;
		read_aes_key(&ctx);
		AES_convert_key(&ctx);

		pos += STGL_HEADER_LEN;

		int buf_len = (len + 15) / 16 * 16;
		if ((buf_len + STGL_HEADER_LEN) > (fill - start)) {
			return -1;
		}
		uint8_t *buf = pos;
		if (*output_buf == NULL) {
			buf = (unsigned char *)malloc(buf_len + 1);
			if (buf == NULL) {
				printf("memory is not enough\n");
				return -1;
			}
			memset(buf, 0, buf_len);
		}

		AES_cbc_decrypt(&ctx, pos, buf, buf_len);
		for (int i = len; i < (buf_len + 1); i++) {
			buf[i] = '\0';
		}
		*output_buf = buf;
		return len;
	}
}
