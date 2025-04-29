package com.dsm.utils;

import com.dsm.pojo.dto.ContainerCreateDTO;
import com.dsm.model.dockerApi.ContainerCreateRequest;
import com.github.dockerjava.api.model.*;

import java.util.List;
import java.util.stream.Collectors;

public class ContainerCreateDTOConverterV2 {

    public static ContainerCreateRequest convert(ContainerCreateDTO dto) {
        ContainerCreateRequest req = new ContainerCreateRequest();

        // ========= 基本配置 =========
        req.setName(dto.getName());
        req.setHostName(dto.getHostName());
        req.setDomainName(dto.getDomainName());
        req.setUser(dto.getUser());
        req.setImage(dto.getImage());
        req.setCmd(dto.getCmd());
        req.setEntrypoint(dto.getEntrypoint());
        req.setEnv(dto.getEnv());
        req.setWorkingDir(dto.getWorkingDir());

        // ========= 卷挂载 =========
        if (dto.getVolumePaths() != null) {
            List<Volume> volumes = dto.getVolumePaths().stream().map(Volume::new).collect(Collectors.toList());
            req.setVolumes(volumes);
        }

        if (dto.getBinds() != null) {
            List<Bind> binds = dto.getBinds().stream().map(Bind::parse).collect(Collectors.toList());
            req.setBinds(binds);
        }

        if (dto.getVolumesFrom() != null) {
            VolumesFrom[] vfs = dto.getVolumesFrom().stream().map(VolumesFrom::new).toArray(VolumesFrom[]::new);
            req.setVolumesFrom(vfs);
        }

        // ========= 网络配置 =========
        req.setNetworkMode(dto.getNetworkMode());
        if (dto.getLinks() != null) {
            req.setLinks(dto.getLinks().toArray(new String[0]));
        }

        if (dto.getExposedPorts() != null) {
            List<ExposedPort> exposedPorts = dto.getExposedPorts().stream()
                    .map(ExposedPort::tcp).collect(Collectors.toList());
            req.setExposedPorts(exposedPorts);
        }

        if (dto.getPortBindings() != null) {
            Ports portBindings = new Ports();
            dto.getPortBindings().forEach((hostPort, containerPort) -> {
                portBindings.bind(ExposedPort.tcp(containerPort), Ports.Binding.bindPort(hostPort));
            });
            req.setPortBindings(portBindings);
        }

        req.setDns(dto.getDns());
        req.setDnsSearch(dto.getDnsSearch());
        req.setExtraHosts(dto.getExtraHosts());

        req.setNetworks(dto.getNetworks());
        req.setIpv4Address(dto.getIpv4Address());
        req.setIpv6Address(dto.getIpv6Address());
        req.setAliases(dto.getAliases());

        // ========= HostConfig 权限与日志 =========
        req.setCapAdd(dto.getCapAdd());
        req.setCapDrop(dto.getCapDrop());

        if (dto.getDevices() != null) {
            Device[] devices = dto.getDevices().stream().map(d ->
                    new Device(d.getCgroupPermissions(), d.getPathOnHost(), d.getPathInContainer())
            ).toArray(Device[]::new);
            req.setDevices(devices);
        }

        if (dto.getRestartPolicy() != null) {
            req.setRestartPolicy(RestartPolicy.parse(dto.getRestartPolicy()));
        }

        req.setLogDriver(dto.getLogDriver());
        req.setLogOptions(dto.getLogOptions());

        // ========= 资源限制 =========
        req.setMemory(dto.getMemory());
        req.setMemorySwap(dto.getMemorySwap());
        req.setCpuShares(dto.getCpuShares());
        req.setCpuPeriod(dto.getCpuPeriod());
        req.setCpuQuota(dto.getCpuQuota());
        req.setCpusetCpus(dto.getCpusetCpus());

        // ========= 运行行为 =========
        req.setTty(dto.getTty());
        req.setStdinOpen(dto.getStdinOpen());
        req.setAttachStdin(dto.getAttachStdin());
        req.setAttachStdout(dto.getAttachStdout());
        req.setAttachStderr(dto.getAttachStderr());
        req.setPrivileged(dto.getPrivileged());
        req.setPublishAllPorts(dto.getPublishAllPorts());
        req.setAutoRemove(dto.getAutoRemove());

        // ========= 安全配置 =========
        if (dto.getSecurityOpts() != null) {
            req.setSecurityOpts(dto.getSecurityOpts().toArray(new String[0]));
        }

        // ========= 元数据 =========
        req.setLabels(dto.getLabels());

        // ========= 限制与检查 =========
        if (dto.getUlimits() != null) {
            Ulimit[] ulimits = dto.getUlimits().stream().map(u ->
                    new Ulimit(u.getName(), u.getSoft(), u.getHard())
            ).toArray(Ulimit[]::new);
            req.setUlimits(ulimits);
        }

        if (dto.getHealthCheck() != null) {
            ContainerCreateDTO.HealthCheckConfig hc = dto.getHealthCheck();
            HealthCheck healthCheck = new HealthCheck();
            healthCheck.withTest(hc.getTest());
            healthCheck.withInterval(hc.getInterval());
            healthCheck.withTimeout(hc.getTimeout());
            healthCheck.withStartPeriod(hc.getStartPeriod());
            healthCheck.withRetries(hc.getRetries());
            req.setHealthCheck(healthCheck);
        }

        // ========= 其他高级配置 =========
        req.setStopSignal(dto.getStopSignal());
        req.setStopTimeout(dto.getStopTimeout());
        req.setOomKillDisable(dto.getOomKillDisable());
        req.setShmSize(dto.getShmSize());

        return req;
    }
}