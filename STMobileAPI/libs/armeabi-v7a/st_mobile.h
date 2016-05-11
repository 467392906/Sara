
#ifndef INCLUDE_STMOBILE_API_ST_MOBILE_H_
#define INCLUDE_STMOBILE_API_ST_MOBILE_H_

#include "cv_common.h"
/// @defgroup st_mobile_common st_mobile common
/// @brief Common definitions for st_mobile
/// @{

//>> CONFIG_ST_MOBILE_API_TRACK_106
/// @defgroup st_mobile_track face 106 points track
/// @brief face 106 points tracking interfaces
///
/// This set of interfaces processing face 106 points tracking routines
///
/// @{
/// @brief  图像预处理
typedef enum {
	CV_CLOCKWISE_ROTATE_0 = 0,	///< 图像不需要转向
	CV_CLOCKWISE_ROTATE_90 = 1,	///< 图像需要顺时针旋转90度
	CV_CLOCKWISE_ROTATE_180 = 2,	///< 图像需要顺时针旋转180度
	CV_CLOCKWISE_ROTATE_270 = 3	///< 图像需要顺时针旋转270度
} cv_rotate_type;

/// @brief 供106点track使用
typedef struct st_mobile_106_t {
	cv_rect_t rect;			///< 代表面部的矩形区域
	float score;			///< 置信度
	cv_pointf_t points_array[106];	///< 人脸106关键点的数组
	int yaw;			///< 水平转角，真实度量的左负右正
	int pitch;			///< 俯仰角，真实度量的上负下正
	int roll;			///< 旋转角，真实度量的左负右正
	int eye_dist;			///< 两眼间距
	int ID;				///< faceID
} st_mobile_106_t;


/// @brief 创建实时人脸106关键点跟踪句柄
/// @param model_path 模型文件的绝对路径或相对路径，若不指定模型可为NULL; 模型中包含detect+align+pose模型
/// @parma[out] handle 人脸跟踪句柄，失败返回NULL
/// @return 成功返回CV_OK, 失败返回其他错误信息
CV_SDK_API cv_result_t
st_mobile_tracker_106_create(
	const char* model_path,
	cv_handle_t* handle
);


/// @brief 设置检测到的最大人脸数目N，持续track已检测到的N个人脸直到人脸数小于N再继续做detect.
/// @param max_facecount 设置为1即是单脸跟踪，有效范围为[1, 32]
/// @param 成功返回CV_OK, 错误则返回错误码
CV_SDK_API
cv_result_t st_mobile_tracker_106_set_facelimit(
	cv_handle_t handle,
	int max_facecount
);

/// @brief 设置tracker每多少帧进行一次detect.
/// @param val  有效范围[1, -)
/// @param 成功返回CV_OK, 错误则返回错误码
CV_SDK_API
cv_result_t st_mobile_tracker_106_set_detectinternal(
	cv_handle_t handle,
	int val
);

/// @brief 对连续视频帧进行实时快速人脸106关键点跟踪
/// @param handle 已初始化的实时人脸跟踪句柄
/// @param image 用于检测的图像数据
/// @param pixel_format 用于检测的图像数据的像素格式,都支持，不推荐BGRA和BGR，会慢
/// @param image_width 用于检测的图像的宽度(以像素为单位)
/// @param image_height 用于检测的图像的高度(以像素为单位)
/// @param orientation 视频中人脸的方向
/// @param p_faces_array 检测到的人脸信息数组，api负责分配内存，需要调用st_mobile_release_tracker_result函数释放
/// @param p_faces_count 检测到的人脸数量
/// @return 成功返回CV_OK，否则返回错误类型
CV_SDK_API cv_result_t
st_mobile_tracker_106_track(
	cv_handle_t handle,
	const unsigned char *image,
	cv_pixel_format pixel_format,
	int image_width,
	int image_height,
	int image_stride,
	cv_rotate_type orientation,
	st_mobile_106_t **p_faces_array,
	int *p_faces_count
);

/// @brief 释放实时人脸106关键点跟踪返回结果时分配的空间
/// @param faces_array 跟踪到到的人脸信息数组
/// @param faces_count 跟踪到的人脸数量
CV_SDK_API void
st_mobile_tracker_106_release_result(
	st_mobile_106_t *faces_array,
	int faces_count
);

/// @brief 销毁已初始化的track106句柄
/// @param handle 已初始化的句柄
CV_SDK_API void
st_mobile_tracker_106_destroy(
	cv_handle_t handle
);
/// @}

//>> CONFIG_ST_MOBILE_API_CONVERT
/// 支持的颜色转换格式
typedef enum {
	CV_BGRA_YUV420P = 0,	///< CV_PIX_FMT_BGRA8888到CV_PIX_FMT_YUV420P转换
	CV_BGR_YUV420P = 1,		///< CV_PIX_FMT_BGR888到CV_PIX_FMT_YUV420P转换
	CV_BGRA_NV12 = 2,		///< CV_PIX_FMT_BGRA8888到CV_PIX_FMT_NV12转换
	CV_BGR_NV12 = 3,		///< CV_PIX_FMT_BGR888到CV_PIX_FMT_NV12转换
	CV_BGRA_NV21 = 4,		///< CV_PIX_FMT_BGRA8888到CV_PIX_FMT_NV21转换
	CV_BGR_NV21 = 5,		///< CV_PIX_FMT_BGR888到CV_PIX_FMT_NV21转换
	CV_YUV420P_BGRA = 6,	///< CV_PIX_FMT_YUV420P到CV_PIX_FMT_BGRA8888转换
	CV_YUV420P_BGR = 7,		///< CV_PIX_FMT_YUV420P到CV_PIX_FMT_BGR888转换
	CV_NV12_BGRA = 8,		///< CV_PIX_FMT_NV12到CV_PIX_FMT_BGRA8888转换
	CV_NV12_BGR = 9,		///< CV_PIX_FMT_NV12到CV_PIX_FMT_BGR888转换
	CV_NV21_BGRA = 10,		///< CV_PIX_FMT_NV21到CV_PIX_FMT_BGRA8888转换
	CV_NV21_BGR = 11,		///< CV_PIX_FMT_NV21到CV_PIX_FMT_BGR888转换
	CV_BGRA_GRAY = 12,		///< CV_PIX_FMT_BGRA8888到CV_PIX_FMT_GRAY8转换
	CV_BGR_BGRA = 13,		///< CV_PIX_FMT_BGR888到CV_PIX_FMT_BGRA8888转换
	CV_BGRA_BGR = 14,		///< CV_PIX_FMT_BGRA8888到CV_PIX_FMT_BGR888转换
	CV_YUV420P_GRAY = 15,	///< CV_PIX_FMT_YUV420P到CV_PIX_FMT_GRAY8转换
	CV_NV12_GRAY = 16,		///< CV_PIX_FMT_NV12到CV_PIX_FMT_GRAY8转换
	CV_NV21_GRAY = 17,		///< CV_PIX_FMT_NV21到CV_PIX_FMT_GRAY8转换
	CV_BGR_GRAY = 18,		///< CV_PIX_FMT_BGR888到CV_PIX_FMT_GRAY8转换
	CV_GRAY_YUV420P = 19,	///< CV_PIX_FMT_GRAY8到CV_PIX_FMT_YUV420P转换
	CV_GRAY_NV12 = 20,		///< CV_PIX_FMT_GRAY8到CV_PIX_FMT_NV12转换
	CV_GRAY_NV21 = 21,		///< CV_PIX_FMT_GRAY8到CV_PIX_FMT_NV21转换
	CV_NV12_YUV420P= 22,	///< CV_PIX_FMT_GRAY8到CV_PIX_FMT_NV21转换
	CV_NV21_YUV420P = 23,	///< CV_PIX_FMT_GRAY8到CV_PIX_FMT_NV21转换
	CV_NV21_RGBA = 24,		///< CV_PIX_FMT_NV21到CV_PIX_FMT_RGBA转换
	CV_BGR_RGBA = 25,		///< CV_PIX_FMT_BGR到CV_PIX_FMT_RGBA转换
	CV_BGRA_RGBA = 26,		///< CV_PIX_FMT_BGRA到CV_PIX_FMT_RGBA转换
	CV_RGBA_BGRA = 27		///< CV_PIX_FMT_RGBA到CV_PIX_FMT_BGRA转换
} cv_color_convert_type;

/// @brief 进行颜色格式转换, 不建议使用关于YUV420P的转换，速度较慢
/// @param image_src 用于待转换的图像数据
/// @param image_dst 转换后的图像数据
/// @param image_width 用于转换的图像的宽度(以像素为单位)
/// @param image_height 用于转换的图像的高度(以像素为单位)
/// @param type 需要转换的颜色格式
/// @return 正常返回CV_OK，否则返回错误类型
CV_SDK_API cv_result_t
st_mobile_color_convert(
	const unsigned char *image_src,
	unsigned char *image_dst,
	int image_width,
	int image_height,
	cv_color_convert_type type
);

//>> CONFIG_API_END__

#endif  // INCLUDE_STMOBILE_API_ST_MOBILE_H_
