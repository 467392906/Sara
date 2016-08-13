
#ifndef INCLUDE_STMOBILE_API_ST_MOBILE_STIKER_H_
#define INCLUDE_STMOBILE_API_ST_MOBILE_STIKER_H_
//>> CONFIG_ST_MOBILE_API_STICKER
#include "st_common.h"
#include "st_mobile_human_action.h"

/// @defgroup st_mobile_sticker
/// @brief sticker for image interfaces
///
/// This set of interfaces sticker.
///

/// @brief 创建贴纸句柄
/// @param[in] dirpath 输入的素材包文件夹路径
/// @parma[out] handle 贴纸句柄，失败返回NULL
/// @return 成功返回ST_OK, 失败返回其他错误码,错误码定义在st_common.h 中，如ST_E_FAIL等
ST_SDK_API st_result_t
st_mobile_sticker_create(
	const char* dir_path,
	st_handle_t *handle
);

/// @brief 更换素材包,删除原有素材包
/// @parma[in] handle 已初始化的贴纸句柄
/// @param[in] dir_path 待更换的素材包文件夹路径
/// @return 成功返回ST_OK, 失败返回其他错误码,错误码定义在st_common.h 中，如ST_E_FAIL等
ST_SDK_API st_result_t
st_mobile_sticker_change_package(
	st_handle_t handle,
	const char* dir_path
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


//>> CONFIG_API_END__

#endif  // INCLUDE_STMOBILE_API_ST_MOBILE_H_
