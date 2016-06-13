#ifndef STBEAUTY_GETSCRIPT_H
#define STBEAUTY_GETSCRIPT_H

#include <android/log.h>
#include "stmobile_crypto.h"

int stgl_decrypt2mem(unsigned char *start, unsigned char *fill,
		     unsigned char **output_buf);

#endif
