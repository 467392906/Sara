
#ifndef INCLUDE_STMOBILE_API_ST_MOBILE_H_
#define INCLUDE_STMOBILE_API_ST_MOBILE_H_

#include "st_common.h"
/// @defgroup st_mobile_common st_mobile common
/// @brief Common definitions for st_mobile
/// @{
/// @brief 创建美颜句柄
/// @param [in] image_width 图像宽度
/// @param [in] image_height  图像高度
/// @param [out] handle 初始化的美颜句柄
/// @return 成功返回ST_OK, 错误则返回错误码
ST_SDK_API st_result_t
st_mobile_beautify_create(
	int image_width, int image_height,
	st_handle_t *handle
);
///@brief 美颜参数类型
typedef enum {
	ST_BEAUTIFY_DEFRECKLE = 0,		/// 是否加祛斑功能, 0表示false，1表示true
	ST_BEAUTIFY_CONTRAST_STRENGTH =1 ,	/// 对比度强度，[0,1]
	ST_BEAUTIFY_TONE_STRENGTH = 2,		/// 色调强度， [0,1]
	ST_BEAUTIFY_SMOOTH_STRENGTH = 3,	/// 平滑强度， [0,1]
} st_beautify_type;

/// @brief 设置美颜参数，默认包含所有功能参数和强度参数
/// @param [in] handle 已初始化的美颜句柄
/// @param [in] type 美颜参数关键字， 例如ST_BEAUTIFY_DEFRECKLE、 ST_BEAUTIFY_TONE_STRENGTH等
/// @param [in] value 参数取值
/// @return 成功返回ST_OK, 错误则返回错误码,错误码定义在st_common.h 中，如ST_E_FAIL等
ST_SDK_API st_result_t
st_mobile_beautify_setparam(
	st_handle_t handle,
	st_beautify_type type,
	float value
);

/// @brief 对图像buffer做美颜美颜处理
/// @param[in] handle 已初始化的美颜句柄
/// @param[in] img_in 输入图片的数据数组
/// @param[in] fmt_in 输入图片的类型,支持NV21,BGR,BGRA,NV12,RGBA格式。
/// @param[in] image_width 输入图片的宽度(以像素为单位)
/// @param[in] image_height 输入图片的高度(以像素为单位)
/// @param[in] image_stride 用于检测的图像的跨度(以像素为单位)，即每行的字节数；目前仅支持字节对齐的padding，不支持roi
/// @param[out] img_out 输出图像数据数组
/// @param[in] fmt_out 输出图片的类型,支持NV21,BGR,BGRA,NV12,RGBA格式。
/// @return 成功返回ST_OK, 错误则返回错误码，错误码定义在st_common.h 中，如ST_E_FAIL等
ST_SDK_API st_result_t
st_mobile_beautify_process_buffer(
	st_handle_t handle,
	const unsigned char* img_in, st_pixel_format fmt_in,
	int image_width, int image_height, int image_stride,
	unsigned char* img_out, st_pixel_format fmt_out
);

/// @brief 对图像做美颜美颜处理，此接口针对不在opengl环境中执行函数的用户
/// @param[in] handle 已初始化的美颜句柄
/// @param[in] img_in 输入图片的数据数组
/// @param[in] fmt_in 输入图片的类型,支持NV21,BGR,BGRA,NV12,RGBA格式。
/// @param[in] image_width 输入图片的宽度(以像素为单位)
/// @param[in] image_height 输入图片的高度(以像素为单位)
/// @param[in] image_stride 用于检测的图像的跨度(以像素为单位)，即每行的字节数；目前仅支持字节对齐的padding，不支持roi
/// @param[out] img_out 输出图像数据数组
/// @param[in] fmt_out 输出图片的类型,支持NV21,BGR,BGRA,NV12,RGBA格式。
/// @return 成功返回ST_OK, 错误则返回错误码，错误码定义在st_common.h 中，如ST_E_FAIL等
ST_SDK_API st_result_t
st_mobile_beautify_process_picture(
	st_handle_t handle,
	const unsigned char* img_in, st_pixel_format fmt_in,
	int image_width, int image_height, int image_stride,
	unsigned char* img_out, st_pixel_format fmt_out
);

/// @brief 对NV12,NV21图像做美颜美颜处理，此接口针对ios平台中Y数据块与UV数据块不连续存储的数据，例如分辨率1920*1080的图像
/// @param[in] handle 已初始化的美颜句柄
/// @param[in] img_in_y 输入图片的Y数据指针
/// @param[in] img_in_uv 输入图片的UV数据指针
/// @param[in] fmt_in 输入图片的类型,支持NV21,NV12格式。
/// @param[in] image_width 输入图片的宽度(以像素为单位)
/// @param[in] image_height 输入图片的高度(以像素为单位)
/// @param[in] image_stride 用于检测的图像的跨度(以像素为单位)，即每行的字节数；目前仅支持字节对齐的padding，不支持roi
/// @param[out] img_out 输出图像数据数组
/// @param[in] fmt_out 输出图片的类型,支持NV21,BGR,BGRA,NV12,RGBA格式。
/// @return 成功返回ST_OK, 错误则返回错误码，错误码定义在st_common.h 中，如ST_E_FAIL等
ST_SDK_API st_result_t
st_mobile_beautify_process_nvbuffer(
	st_handle_t handle,
	const unsigned char* img_in_y, unsigned char* img_in_uv, st_pixel_format fmt_in,
	int image_width, int image_height, int image_stride,
	unsigned char* img_out, st_pixel_format fmt_out
);


/// @brief 对OpenGL 纹理进行美颜处理
/// @param[in] handle 已初始化的美颜句柄
/// @param[in] textureid_src 待处理的纹理id, 仅支持RGBA纹理
/// @param[in] image_width 输入纹理的宽度(以像素为单位)
/// @param[in] image_height 输入纹理的高度(以像素为单位)
/// @param[in] textureid_dst 处理后的纹理id
/// @return 成功返回ST_OK, 错误则返回错误码，错误码定义在st_common.h 中，如ST_E_FAIL等
ST_SDK_API st_result_t
st_mobile_beautify_process_texture(
	st_handle_t handle,
	unsigned int textureid_src,
	int image_width, int image_height,
	unsigned int textureid_dst
);

/// @brief 释放美颜句柄
/// @param[in] handle 已初始化的美颜句柄
ST_SDK_API void
st_mobile_beautify_destroy(
	st_handle_t handle
);


/// 支持的颜色转换格式
typedef enum {
	ST_BGRA_YUV420P = 0,	///< ST_PIX_FMT_BGRA8888到ST_PIX_FMT_YUV420P转换
	ST_BGR_YUV420P = 1,		///< ST_PIX_FMT_BGR888到ST_PIX_FMT_YUV420P转换
	ST_BGRA_NV12 = 2,		///< ST_PIX_FMT_BGRA8888到ST_PIX_FMT_NV12转换
	ST_BGR_NV12 = 3,		///< ST_PIX_FMT_BGR888到ST_PIX_FMT_NV12转换
	ST_BGRA_NV21 = 4,		///< ST_PIX_FMT_BGRA8888到ST_PIX_FMT_NV21转换
	ST_BGR_NV21 = 5,		///< ST_PIX_FMT_BGR888到ST_PIX_FMT_NV21转换
	ST_YUV420P_BGRA = 6,	///< ST_PIX_FMT_YUV420P到ST_PIX_FMT_BGRA8888转换
	ST_YUV420P_BGR = 7,		///< ST_PIX_FMT_YUV420P到ST_PIX_FMT_BGR888转换
	ST_NV12_BGRA = 8,		///< ST_PIX_FMT_NV12到ST_PIX_FMT_BGRA8888转换
	ST_NV12_BGR = 9,		///< ST_PIX_FMT_NV12到ST_PIX_FMT_BGR888转换
	ST_NV21_BGRA = 10,		///< ST_PIX_FMT_NV21到ST_PIX_FMT_BGRA8888转换
	ST_NV21_BGR = 11,		///< ST_PIX_FMT_NV21到ST_PIX_FMT_BGR888转换
	ST_BGRA_GRAY = 12,		///< ST_PIX_FMT_BGRA8888到ST_PIX_FMT_GRAY8转换
	ST_BGR_BGRA = 13,		///< ST_PIX_FMT_BGR888到ST_PIX_FMT_BGRA8888转换
	ST_BGRA_BGR = 14,		///< ST_PIX_FMT_BGRA8888到ST_PIX_FMT_BGR888转换
	ST_YUV420P_GRAY = 15,	///< ST_PIX_FMT_YUV420P到ST_PIX_FMT_GRAY8转换
	ST_NV12_GRAY = 16,		///< ST_PIX_FMT_NV12到ST_PIX_FMT_GRAY8转换
	ST_NV21_GRAY = 17,		///< ST_PIX_FMT_NV21到ST_PIX_FMT_GRAY8转换
	ST_BGR_GRAY = 18,		///< ST_PIX_FMT_BGR888到ST_PIX_FMT_GRAY8转换
	ST_GRAY_YUV420P = 19,	///< ST_PIX_FMT_GRAY8到ST_PIX_FMT_YUV420P转换
	ST_GRAY_NV12 = 20,		///< ST_PIX_FMT_GRAY8到ST_PIX_FMT_NV12转换
	ST_GRAY_NV21 = 21,		///< ST_PIX_FMT_GRAY8到ST_PIX_FMT_NV21转换
	ST_NV12_YUV420P= 22,	///< ST_PIX_FMT_GRAY8到ST_PIX_FMT_NV21转换
	ST_NV21_YUV420P = 23,	///< ST_PIX_FMT_GRAY8到ST_PIX_FMT_NV21转换
	ST_NV21_RGBA = 24,		///< ST_PIX_FMT_NV21到ST_PIX_FMT_RGBA转换
	ST_BGR_RGBA = 25,		///< ST_PIX_FMT_BGR到ST_PIX_FMT_RGBA转换
	ST_BGRA_RGBA = 26,		///< ST_PIX_FMT_BGRA到ST_PIX_FMT_RGBA转换
	ST_RGBA_BGRA = 27,		///< ST_PIX_FMT_RGBA到ST_PIX_FMT_BGRA转换
	ST_GRAY_BGR = 28,		///< ST_PIX_FMT_GRAY8到ST_PIX_FMT_BGR888转换
	ST_GRAY_BGRA = 29,		///< ST_PIX_FMT_GRAY8到ST_PIX_FMT_BGRA8888转换
	ST_NV12_RGBA = 30,		///< ST_PIX_FMT_NV12到ST_PIX_FMT_RGBA8888转换
	ST_NV12_RGB = 31,		///< ST_PIX_FMT_NV12到ST_PIX_FMT_RGB888转换
	ST_RGBA_NV12 = 32,		///< ST_PIX_FMT_RGBA8888到ST_PIX_FMT_NV12转换
	ST_RGB_NV12 = 33,		///< ST_PIX_FMT_RGB888到ST_PIX_FMT_NV12转换
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


#endif  // INCLUDE_STMOBILE_API_ST_MOBILE_H_
