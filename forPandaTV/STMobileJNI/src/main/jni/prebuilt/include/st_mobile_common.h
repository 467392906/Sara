#ifndef ST_COMMON_H_
#define ST_COMMON_H_

/// @defgroup st_common st common
/// @brief common definitions for st libs
/// @{


#ifdef _MSC_VER
#	ifdef __cplusplus
#		ifdef ST_STATIC_LIB
#			define ST_SDK_API  extern "C"
#		else
#			ifdef SDK_EXPORTS
#				define ST_SDK_API extern "C" __declspec(dllexport)
#			else
#				define ST_SDK_API extern "C" __declspec(dllimport)
#			endif
#		endif
#	else
#		ifdef ST_STATIC_LIB
#			define ST_SDK_API
#		else
#			ifdef SDK_EXPORTS
#				define ST_SDK_API __declspec(dllexport)
#			else
#				define ST_SDK_API __declspec(dllimport)
#			endif
#		endif
#	endif
#else /* _MSC_VER */
#	ifdef __cplusplus
#		ifdef SDK_EXPORTS
#			define ST_SDK_API extern "C" __attribute__((visibility ("default")))
#		else
#			define ST_SDK_API extern "C"
#		endif
#	else
#		ifdef SDK_EXPORTS
#			define ST_SDK_API __attribute__((visibility ("default")))
#		else
#			define ST_SDK_API
#		endif
#	endif
#endif

/// st handle declearation
typedef void *st_handle_t;

/// st result declearation
typedef int   st_result_t;

#define ST_OK (0)                       ///< 正常运行
#define ST_E_INVALIDARG (-1)            ///< 无效参数
#define ST_E_HANDLE (-2)                ///< 句柄错误
#define ST_E_OUTOFMEMORY (-3)           ///< 内存不足
#define ST_E_FAIL (-4)                  ///< 内部错误
#define ST_E_DELNOTFOUND (-5)           ///< 定义缺失
#define ST_E_INVALID_PIXEL_FORMAT (-6)  ///< 不支持的图像格式
#define ST_E_FILE_NOT_FOUND (-10)       ///< 模型文件不存在
#define ST_E_INVALID_FILE_FORMAT (-11)  ///< 模型格式不正确，导致加载失败
#define ST_E_INVALID_APPID (-12)        ///< 包名错误
#define ST_E_INVALID_AUTH (-13)         ///< license功能不支持
#define ST_E_AUTH_EXPIRE (-14)          ///< license或SDK过期
#define ST_E_FILE_EXPIRE (-15)          ///< 模型文件过期
#define ST_E_DONGLE_EXPIRE (-16)        ///< 加密狗过期
#define ST_E_ONLINE_AUTH_FAIL (-17)     ///< 在线验证失败
#define ST_E_ONLINE_AUTH_TIMEOUT (-18)  ///< 在线验证超时
#define ST_E_INVALID_ACTIVATE (-19)     ///< 产品未激活
#define ST_E_INVALID_LICENSE (-20)      ///< license文件无效
#define ST_E_NO_CAPABILITY (-21)        ///< license文件没有提供这个能力

/// st rectangle definition
typedef struct st_rect_t {
	int left;   ///< 矩形最左边的坐标
	int top;    ///< 矩形最上边的坐标
	int right;  ///< 矩形最右边的坐标
	int bottom; ///< 矩形最下边的坐标
} st_rect_t;

/// st float type point definition
typedef struct st_pointf_t {
	float x;    ///< 点的水平方向坐标，为浮点数
	float y;    ///< 点的竖直方向坐标，为浮点数
} st_pointf_t;

/// st integer type point definition
typedef struct st_pointi_t {
	int x;      ///< 点的水平方向坐标，为整数
	int y;      ///< 点的竖直方向坐标，为整数
} st_pointi_t;

/// st pixel format definition
typedef enum {
	ST_PIX_FMT_GRAY8,   ///< Y    1        8bpp ( 单通道8bit灰度像素 )
	ST_PIX_FMT_YUV420P, ///< YUV  4:2:0   12bpp ( 3通道, 一个亮度通道, 另两个为U分量和V分量通道, 所有通道都是连续的 )
	ST_PIX_FMT_NV12,    ///< YUV  4:2:0   12bpp ( 2通道, 一个通道是连续的亮度通道, 另一通道为UV分量交错 )
	ST_PIX_FMT_NV21,    ///< YUV  4:2:0   12bpp ( 2通道, 一个通道是连续的亮度通道, 另一通道为VU分量交错 )
	ST_PIX_FMT_BGRA8888,///< BGRA 8:8:8:8 32bpp ( 4通道32bit BGRA 像素 )
	ST_PIX_FMT_BGR888,  ///< BGR  8:8:8   24bpp ( 3通道24bit BGR 像素 )
	ST_PIX_FMT_RGBA8888 ///< BGRA 8:8:8:8 32bpp ( 4通道32bit RGBA 像素 )
} st_pixel_format;

typedef enum {
	ST_CLOCKWISE_ROTATE_0 = 0,  ///< 图像不需要转向
	ST_CLOCKWISE_ROTATE_90 = 1, ///< 图像需要顺时针旋转90度
	ST_CLOCKWISE_ROTATE_180 = 2,///< 图像需要顺时针旋转180度
	ST_CLOCKWISE_ROTATE_270 = 3 ///< 图像需要顺时针旋转270度
} st_rotate_type;

/// @brief 供106点使用
typedef struct st_mobile_106_t {
    st_rect_t rect;         ///< 代表面部的矩形区域
    float score;            ///< 置信度
    st_pointf_t points_array[106];  ///< 人脸106关键点的数组
    float yaw;              ///< 水平转角，真实度量的左负右正
    float pitch;            ///< 俯仰角，真实度量的上负下正
    float roll;             ///< 旋转角，真实度量的左负右正
    float eye_dist;         ///< 两眼间距
    int ID;                 ///< faceID
} st_mobile_106_t,*p_st_mobile_106_t;

/// face信息及face上的相关动作
typedef struct st_mobile_face_action_t {
	struct st_mobile_106_t face;    /// 人脸信息，包含矩形、106点、pose信息等
	unsigned int face_action;       /// 脸部动作
} st_mobile_face_action_t;

// face action 0x00000000-0x000000FF
#define ST_MOBILE_FACE_DETECT      0x00000001    ///<  人脸检测
#define ST_MOBILE_EYE_BLINK        0x00000002    ///<  眨眼
#define ST_MOBILE_MOUTH_AH         0x00000004    ///<  嘴巴大张
#define ST_MOBILE_HEAD_YAW         0x00000008    ///<  摇头
#define ST_MOBILE_HEAD_PITCH       0x00000010    ///<  点头
#define ST_MOBILE_BROW_JUMP        0x00000020    ///<  眉毛挑动

/// 支持的颜色转换格式
typedef enum {
	ST_BGRA_YUV420P = 0,    ///< ST_PIX_FMT_BGRA8888到ST_PIX_FMT_YUV420P转换
	ST_BGR_YUV420P = 1,     ///< ST_PIX_FMT_BGR888到ST_PIX_FMT_YUV420P转换
	ST_BGRA_NV12 = 2,       ///< ST_PIX_FMT_BGRA8888到ST_PIX_FMT_NV12转换
	ST_BGR_NV12 = 3,        ///< ST_PIX_FMT_BGR888到ST_PIX_FMT_NV12转换
	ST_BGRA_NV21 = 4,       ///< ST_PIX_FMT_BGRA8888到ST_PIX_FMT_NV21转换
	ST_BGR_NV21 = 5,        ///< ST_PIX_FMT_BGR888到ST_PIX_FMT_NV21转换
	ST_YUV420P_BGRA = 6,    ///< ST_PIX_FMT_YUV420P到ST_PIX_FMT_BGRA8888转换
	ST_YUV420P_BGR = 7,     ///< ST_PIX_FMT_YUV420P到ST_PIX_FMT_BGR888转换
	ST_NV12_BGRA = 8,       ///< ST_PIX_FMT_NV12到ST_PIX_FMT_BGRA8888转换
	ST_NV12_BGR = 9,        ///< ST_PIX_FMT_NV12到ST_PIX_FMT_BGR888转换
	ST_NV21_BGRA = 10,      ///< ST_PIX_FMT_NV21到ST_PIX_FMT_BGRA8888转换
	ST_NV21_BGR = 11,       ///< ST_PIX_FMT_NV21到ST_PIX_FMT_BGR888转换
	ST_BGRA_GRAY = 12,      ///< ST_PIX_FMT_BGRA8888到ST_PIX_FMT_GRAY8转换
	ST_BGR_BGRA = 13,       ///< ST_PIX_FMT_BGR888到ST_PIX_FMT_BGRA8888转换
	ST_BGRA_BGR = 14,       ///< ST_PIX_FMT_BGRA8888到ST_PIX_FMT_BGR888转换
	ST_YUV420P_GRAY = 15,   ///< ST_PIX_FMT_YUV420P到ST_PIX_FMT_GRAY8转换
	ST_NV12_GRAY = 16,      ///< ST_PIX_FMT_NV12到ST_PIX_FMT_GRAY8转换
	ST_NV21_GRAY = 17,      ///< ST_PIX_FMT_NV21到ST_PIX_FMT_GRAY8转换
	ST_BGR_GRAY = 18,       ///< ST_PIX_FMT_BGR888到ST_PIX_FMT_GRAY8转换
	ST_GRAY_YUV420P = 19,   ///< ST_PIX_FMT_GRAY8到ST_PIX_FMT_YUV420P转换
	ST_GRAY_NV12 = 20,      ///< ST_PIX_FMT_GRAY8到ST_PIX_FMT_NV12转换
	ST_GRAY_NV21 = 21,      ///< ST_PIX_FMT_GRAY8到ST_PIX_FMT_NV21转换
	ST_NV12_YUV420P = 22,   ///< ST_PIX_FMT_GRAY8到ST_PIX_FMT_NV21转换
	ST_NV21_YUV420P = 23,   ///< ST_PIX_FMT_GRAY8到ST_PIX_FMT_NV21转换
	ST_NV21_RGBA = 24,      ///< ST_PIX_FMT_NV21到ST_PIX_FMT_RGBA转换
	ST_BGR_RGBA = 25,       ///< ST_PIX_FMT_BGR到ST_PIX_FMT_RGBA转换
	ST_BGRA_RGBA = 26,      ///< ST_PIX_FMT_BGRA到ST_PIX_FMT_RGBA转换
	ST_RGBA_BGRA = 27,      ///< ST_PIX_FMT_RGBA到ST_PIX_FMT_BGRA转换
	ST_GRAY_BGR = 28,       ///< ST_PIX_FMT_GRAY8到ST_PIX_FMT_BGR888转换
	ST_GRAY_BGRA = 29,      ///< ST_PIX_FMT_GRAY8到ST_PIX_FMT_BGRA8888转换
	ST_NV12_RGBA = 30,      ///< ST_PIX_FMT_NV12到ST_PIX_FMT_RGBA8888转换
	ST_NV12_RGB = 31,       ///< ST_PIX_FMT_NV12到ST_PIX_FMT_RGB888转换
	ST_RGBA_NV12 = 32,      ///< ST_PIX_FMT_RGBA8888到ST_PIX_FMT_NV12转换
	ST_RGB_NV12 = 33,       ///< ST_PIX_FMT_RGB888到ST_PIX_FMT_NV12转换
	ST_RGBA_BGR = 34,       ///< ST_PIX_FMT_RGBA888到ST_PIX_FMT_BGR888转换
	ST_BGRA_RGB = 35,       ///< ST_PIX_FMT_BGRA888到ST_PIX_FMT_RGB888转换
	ST_RGBA_GRAY = 36,
	ST_RGB_GRAY = 37,
} st_color_convert_type;

/// @brief 进行颜色格式转换, 不建议使用关于YUV420P的转换，速度较慢
/// @param image_src 用于待转换的图像数据
/// @param image_dst 转换后的图像数据
/// @param image_width 用于转换的图像的宽度(以像素为单位)
/// @param image_height 用于转换的图像的高度(以像素为单位)
/// @param type 需要转换的颜色格式
/// @return 正常返回ST_OK，否则返回错误类型
ST_SDK_API st_result_t
st_mobile_color_convert(
	const unsigned char *image_src,
	unsigned char *image_dst,
	int image_width,
	int image_height,
	st_color_convert_type type
);

/// @brief 根据授权文件生成激活码, 在使用新的license文件时使用
/// @param[in] license_path license文件路径
/// @param[out] active_code 返回当前设备的激活码，由用户分配内存，请分配至少129个字节，建议分配1024字节
/// @param[in，out] active_code_len  输入为active_code的内存大小, 返回当前设备的激活码字节长度
/// @return 正常返回ST_OK，否则返回错误类型
ST_SDK_API st_result_t
st_mobile_generate_activecode(
	const char* license_path,
	char* activation_code,
	int* activation_code_len
);

/// @brief 检查激活码, 必须在所有接口之前调用
/// @param[in] license_path license文件路径
/// @param[in] active_path 当前设备的激活码
/// @return 正常返回ST_OK，否则返回错误类型
ST_SDK_API st_result_t
st_mobile_check_activecode(
	const char* license_path,
	const char* activation_code
);

/// @brief 根据授权文件缓存生成激活码, 在使用新的license文件时调用
/// @param[in] license_buf license文件缓存地址
/// @param[in] license_size license文件缓存大小
/// @param[out] activation_code 返回当前设备的激活码, 由用户分配内存; 请分配至少129个字节, 建议分配1024个字节
/// @param[in, out] activation_code_len 输入为activation_code分配的内存大小, 返回生成的设备激活码的字节长度
/// @return 正常返回ST_OK, 否则返回错误类型
ST_SDK_API st_result_t
st_mobile_generate_activecode_from_buffer(
	const char* license_buf,
	int license_size,
	char* activation_code,
	int* activation_code_len
);

/// @brief 检查激活码, 必须在所有接口之前调用
/// @param[in] license_buf license文件缓存
/// @param[in] license_size license文件缓存大小
/// @param[in] activation_code 当前设备的激活码
/// @return 正常返回ST_OK, 否则返回错误类型
ST_SDK_API st_result_t
st_mobile_check_activecode_from_buffer(
	const char* license_buf,
	int license_size,
	const char* activation_code
);

/// @}

#endif  // INCLUDE_ST_COMMON_H_
