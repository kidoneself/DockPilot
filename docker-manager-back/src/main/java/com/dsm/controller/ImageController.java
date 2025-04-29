package com.dsm.controller;

import com.dsm.model.dto.ImageStatusDTO;
import com.dsm.pojo.dto.image.ImageInspectDTO;
import com.dsm.service.ImageService;
import com.dsm.utils.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
@Tag(name = "镜像管理", description = "Docker镜像相关接口")
public class ImageController {

    @Resource
    ImageService imageService;

    /**
     * 删除Docker镜像
     *
     * @param imageId 镜像ID
     * @return 操作结果
     */
    @Operation(summary = "删除镜像", description = "删除指定的Docker镜像")
    @DeleteMapping("/{imageId}")
    public ApiResponse<String> deleteImage(@PathVariable() String imageId, @RequestParam(defaultValue = "true") boolean removeStatus) {
        imageService.removeImage(imageId, removeStatus);
        return ApiResponse.success("删除镜像成功");
    }



    @Operation(summary = "列出镜像", description = "获取所有镜像的列表（包含更新状态信息）")
    @GetMapping()
    public ApiResponse<List<ImageStatusDTO>> listImages() {
        return ApiResponse.success(imageService.listImages());
    }

    /**
     * 更新镜像
     *
     * @param request 更新请求
     * @return 更新结果
     */
    @Operation(summary = "更新镜像", description = "更新指定的镜像到最新版本")
    @PostMapping("/update")
    public ApiResponse<Map<String, Object>> updateImage(@RequestBody Map<String, String> request) {
        String image = request.get("image");
        String tag = request.getOrDefault("tag", "latest");
        Map<String, Object> result = imageService.updateImage(image, tag);
        return ApiResponse.success(result);
    }

    /**
     * 检查镜像更新状态
     *
     * @return 检查结果
     */
    @Operation(summary = "检查镜像更新状态", description = "检查所有镜像是否有新版本可用")
    @PostMapping("/check-updates")
    public ApiResponse<Map<String, Object>> checkImageUpdates() {
        try {
            imageService.checkAllImagesStatus();
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("检查镜像更新状态失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "检查镜像更新状态失败: " + e.getMessage());
        }
    }


    /**
     * 获取镜像详情
     *
     * @return 镜像详情
     */
    @PostMapping("/detail")
    public ApiResponse<ImageInspectDTO> getImageDetail(@RequestBody Map<String, String> request) {
        String imageName = request.get("imageName");
        ImageInspectDTO imageDetail = imageService.getImageDetail(imageName);
        return ApiResponse.success(imageDetail);

    }

}