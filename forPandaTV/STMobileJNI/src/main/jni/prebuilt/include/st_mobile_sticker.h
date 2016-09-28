
#ifndef INCLUDE_STMOBILE_API_ST_MOBILE_STIKER_H_
#define INCLUDE_STMOBILE_API_ST_MOBILE_STIKER_H_

#include <st_mobile_common.h>

/// @defgroup st_mobile_human_action
/// @brief human action detection interfaces
///
/// This set of interfaces detect human action.
///
/// @{

#ifndef CHECK_FLAG
#define CHECK_FLAG(action,flag) (((action)&(flag)) == flag)
#endif

#define ST_MOBILE_HUMAN_ACTION_DEFAULT_CONFIG   0x000000FF  ///<  全部检测
#define ST_MOBILE_HUMAN_ACTION_MAX_FACE_COUNT 10
/// 视频分析结果
typedef struct st_mobile_human_action_t {
	st_mobile_face_action_t faces[ST_MOBILE_HUMAN_ACTION_MAX_FACE_COUNT];   /// 检测到的人脸及动作数组
	int face_count;                                                         /// 检测到的人脸数目
} st_mobile_human_action_t, *p_st_mobile_human_action_t;

/// @brief 创建人体行为检测句柄
/// @param[in] model_path 模型文件的例如models/track.tar
/// @param[in] config 配置选项，例如分别代表每种状态是否被检测，例如ST_MOBILE_DEFAULT_CONFIG 或 ST_MOBILE_HAND_LOVE|ST_MOBILE_FACE_DETECT
/// @parma[out] handle 人体行为检测句柄，失败返回NULL
/// @return 成功返回ST_OK, 失败返回其他错误码,错误码定义在st_mobile_common.h 中，如ST_E_FAIL等
ST_SDK_API st_result_t
st_mobile_human_action_create(
	const char *model_path,
	unsigned int config,
	st_handle_t *handle
	);

/// @brief 释放人体行为检测句柄
/// @param[in] handle 已初始化的人体行为句柄
ST_SDK_API
void st_mobile_human_action_destroy(
	st_handle_t handle
	);

/// @brief 人体行为检测
/// @param[in] handle 已初始化的人体行为句柄
/// @param[in] image 用于检测的图像数据
/// @param[in] pixel_format 用于检测的图像数据的像素格式,都支持
/// @param[in] image_width 用于检测的图像的宽度(以像素为单位)
/// @param[in] image_height 用于检测的图像的高度(以像素为单位)
/// @param[in] image_stride 用于检测的图像的跨度(以像素为单位)，即每行的字节数；目前仅支持字节对齐的padding，不支持roi
/// @param[in] orientation 图像中人脸的方向
/// @param[out] p_humans_array 检测到的人体行为数组，api负责分配内存，需要调用st_mobile_human_release_result函数释放
/// @param[out] p_humans_count 检测到的人数量
/// @return 成功返回ST_OK，失败返回其他错误码,错误码定义在st_mobile_common.h 中，如ST_E_FAIL等
ST_SDK_API st_result_t
st_mobile_human_action_detect(
	st_handle_t handle,
	const unsigned char *image,
	st_pixel_format pixel_format,
	int image_width,
	int image_height,
	int image_stride,
	st_rotate_type orientation,
	unsigned int detect_config,
	st_mobile_human_action_t *p_humans_array
	);

///@brief 重置，清除所有缓存信息
ST_SDK_API st_result_t
st_mobile_human_action_reset(
	st_handle_t handle
	);

/// @defgroup st_mobile_sticker
/// @brief sticker for image interfaces
///
/// This set of interfaces sticker.
///

/// @brief 创建贴纸句柄
/// @param[in] zip_path 输入的素材包路径
/// @parma[out] handle 贴纸句柄，失败返回NULL
/// @return 成功返回ST_OK, 失败返回其他错误码,错误码定义在st_mobile_common.h 中，如ST_E_FAIL等
ST_SDK_API st_result_t
st_mobile_sticker_create(
	const char* zip_path,
	st_handle_t *handle
);

/// @brief 更换素材包,删除原有素材包
/// @parma[in] handle 已初始化的贴纸句柄
/// @param[in] zip_path 待更换的素材包文件夹
/// @return 成功返回ST_OK, 失败返回其他错误码,错误码定义在st_mobile_common.h 中，如ST_E_FAIL等
ST_SDK_API st_result_t
st_mobile_sticker_change_package(
	st_handle_t handle,
	const char* zip_path
);

/// 素材渲染状态
typedef enum {
	ST_MATERIAL_BEGIN = 0,      ///< 开始渲染素材
	ST_MATERIAL_PROCESS = 1,    ///< 素材渲染中
	ST_MATERIAL_END = 2         ///< 素材未被渲染
}st_material_status;

/// 素材渲染状态回调函数
/// @param[in] material_name 素材文件夹名称
/// @param[in] status 素材渲染状态，详见st_material_status定义
typedef void(*item_action)(const char* material_name, st_material_status status);


/// @brief 对OpenGL ES 中的纹理进行贴纸处理，必须在opengl环境中运行，仅支持RGBA图像格式
/// @param[in]textureid_src 输入textureid
/// @param[in] image_width 图像宽度
/// @param[in] image_height 图像高度
/// @param[in] rotate 人脸朝向
/// @param[in] need_mirror 传入图像与显示图像是否是镜像关系
/// @param[in] human_action 动作，包含106点、face动作
/// @param[in] callback 素材渲染回调函数，由用户定义
/// @param[in]textureid_dst 输出textureid
ST_SDK_API st_result_t
st_mobile_sticker_process_texture(
	st_handle_t handle,
	unsigned int textureid_src, int image_width, int image_height,
	st_rotate_type rotate, bool need_mirror,
	p_st_mobile_human_action_t human_action,
	item_action callback,
	unsigned int textureid_dst
);

/// @brief 释放贴纸句柄
ST_SDK_API void
st_mobile_sticker_destroy(
	st_handle_t handle
);



#endif  // INCLUDE_STMOBILE_API_ST_MOBILE_H_
