#ifndef CV_COMMON_H_
#define CV_COMMON_H_

/// @defgroup cv_common cv common
/// @brief common definitions for cv libs
/// @{


#ifdef _MSC_VER
#	ifdef __cplusplus
#		ifdef CV_STATIC_LIB
#			define CV_SDK_API  extern "C"
#		else
#			ifdef SDK_EXPORTS
#				define CV_SDK_API extern "C" __declspec(dllexport)
#			else
#				define CV_SDK_API extern "C" __declspec(dllimport)
#			endif
#		endif
#	else
#		ifdef CV_STATIC_LIB
#			define CV_SDK_API
#		else
#			ifdef SDK_EXPORTS
#				define CV_SDK_API __declspec(dllexport)
#			else
#				define CV_SDK_API __declspec(dllimport)
#			endif
#		endif
#	endif
#else /* _MSC_VER */
#	ifdef __cplusplus
#		ifdef SDK_EXPORTS
#			define CV_SDK_API extern "C" __attribute__((visibility ("default")))
#		else
#			define CV_SDK_API extern "C"
#		endif
#	else
#		ifdef SDK_EXPORTS
#			define CV_SDK_API __attribute__((visibility ("default")))
#		else
#			define CV_SDK_API
#		endif
#	endif
#endif

/// cv handle declearation
typedef void *cv_handle_t;

/// cv result declearation
typedef int   cv_result_t;

#define CV_OK (0)			///< 正常运行
#define CV_E_INVALIDARG (-1)		///< 无效参数
#define CV_E_HANDLE (-2)			///< 句柄错误
#define CV_E_OUTOFMEMORY (-3)		///< 内存不足
#define CV_E_FAIL (-4)			///< 内部错误
#define CV_E_DELNOTFOUND (-5)		///< 定义缺失
#define CV_E_INVALID_PIXEL_FORMAT (-6)	///< 不支持的图像格式
#define CV_E_FILE_NOT_FOUND (-10)	///< 模型文件不存在
#define CV_E_INVALID_FILE_FORMAT (-11)	///< 模型格式不正确，导致加载失败
#define CV_E_INVALID_APPID (-12)		///< 包名错误
#define CV_E_INVALID_AUTH (-13)		///< 加密狗功能不支持
#define CV_E_AUTH_EXPIRE (-14)		///< SDK过期
#define CV_E_FILE_EXPIRE (-15)		///< 模型文件过期
#define CV_E_DONGLE_EXPIRE (-16)		///< 加密狗过期
#define CV_E_ONLINE_AUTH_FAIL (-17)		///< 在线验证失败
#define CV_E_ONLINE_AUTH_TIMEOUT (-18)		///< 在线验证超时


/// cv rectangle definition
typedef struct cv_rect_t {
	int left;	///< 矩形最左边的坐标
	int top;	///< 矩形最上边的坐标
	int right;	///< 矩形最右边的坐标
	int bottom;	///< 矩形最下边的坐标
} cv_rect_t;

/// cv float type point definition
typedef struct cv_pointf_t {
	float x;	///< 点的水平方向坐标，为浮点数
	float y;	///< 点的竖直方向坐标，为浮点数
} cv_pointf_t;

/// cv integer type point definition
typedef struct cv_pointi_t {
	int x;		///< 点的水平方向坐标，为整数
	int y;		///< 点的竖直方向坐标，为整数
} cv_pointi_t;

/// cv pixel format definition
typedef enum {
	CV_PIX_FMT_GRAY8,	///< Y    1        8bpp ( 单通道8bit灰度像素 )
	CV_PIX_FMT_YUV420P,	///< YUV  4:2:0   12bpp ( 3通道, 一个亮度通道, 另两个为U分量和V分量通道, 所有通道都是连续的 )
	CV_PIX_FMT_NV12,	///< YUV  4:2:0   12bpp ( 2通道, 一个通道是连续的亮度通道, 另一通道为UV分量交错 )
	CV_PIX_FMT_NV21,	///< YUV  4:2:0   12bpp ( 2通道, 一个通道是连续的亮度通道, 另一通道为VU分量交错 )
	CV_PIX_FMT_BGRA8888,	///< BGRA 8:8:8:8 32bpp ( 4通道32bit BGRA 像素 )
	CV_PIX_FMT_BGR888	///< BGR  8:8:8   24bpp ( 3通道24bit BGR 像素 )
} cv_pixel_format;

typedef struct cv_feature_header_t {
	int ver;		///< 版本信息
	int idx;		///< 数组下标索引
	int len;		///< CV_FEATURE全部内容的长度，包括feature_header和特征数组，按字节计算，与sizeof(cv_feature_header_t)定义不同
} cv_feature_header_t;

///< cv_feature_header_t为CV_FEATURE数据头解析，实际CV_FEATURE还包含了特征信息
///< cv_feature_t is an opaque structure, use CV_FEATURE_XXX macros to
///< get its size and version
typedef struct cv_feature_t cv_feature_t;

#define CV_FEATURE_HEADER(pf) ((cv_feature_header_t*)(pf))
#define CV_FEATURE_SIZE(pf)   (CV_FEATURE_HEADER(pf)->len)
#define CV_ENCODE_FEATURE_SIZE(pf) ((CV_FEATURE_HEADER(pf)->len+2)/3*4 + 1)

typedef struct cv_clustering_result_t {
	unsigned int count;		///< 目标数量
	int *idxs;			///< 目标索引数组
	unsigned int group_flags;	///< 保留参数
} cv_clustering_result_t;

/// @brief 图像分类标签结果
typedef struct cv_classifier_result_t {
	int id;  ///标签
	float score;  /// 置信度
} cv_classifier_result_t;

/// @}
#endif  // INCLUDE_CV_COMMON_H_
