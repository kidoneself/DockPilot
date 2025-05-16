package com.dsm.utils;

import com.dsm.websocket.sender.WebSocketMessageSender;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class AsyncTaskRunner {

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    // 有返回值的异步任务
    public static <T> CompletableFuture<T> runWithResult(
            Supplier<CompletableFuture<T>> taskSupplier,
            WebSocketMessageSender messageSender,
            WebSocketSession session,
            String taskId) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                return taskSupplier.get().join();
            } catch (Exception e) {
                throw new RuntimeException("异步任务执行失败：" + e.getMessage(), e);
            }
        }, executor).whenComplete((result, ex) -> {
            if (ex != null) {
                System.err.println("异步任务出错：" + ex.getMessage());
                messageSender.sendError(session, taskId, "任务失败：" + ex.getMessage());
            } else {
//                messageSender.sendLog(session, taskId, "任务完成，结果: " + result);
//                messageSender.sendComplete(session, taskId, result);
            }
        });
    }

    // 无返回值的异步任务
    public static CompletableFuture<Void> runWithoutResult(
            Supplier<CompletableFuture<Void>> taskSupplier,
            WebSocketMessageSender messageSender,
            WebSocketSession session,
            String taskId) {

        return CompletableFuture.runAsync(() -> {
                taskSupplier.get().join();
        }, executor).whenComplete((v, ex) -> {
            if (ex != null) {
                messageSender.sendError(session, taskId, "任务失败：" + ex.getMessage());
            } else {
//                messageSender.sendLog(session, taskId, "任务完成");
//                messageSender.sendComplete(session, taskId, true);
            }
        });
    }
}