package com.kobeai.hub.agent.service;

import com.kobeai.hub.agent.memory.LocalFileLongTermMemory;
import io.agentscope.core.ReActAgent;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Agent 会话空闲回收（TTL）单元测试
 */
class AgentServiceSessionTtlTest {

    private static final long MINUTE = 60_000L;

    private AgentService.AgentSession newSession() {
        return new AgentService.AgentSession(
                mock(ReActAgent.class), mock(ModelRouter.class), "deepseek-v4-flash");
    }

    private AgentService newService() {
        AgentService service = new AgentService(
                mock(ModelRouter.class), mock(RagService.class), null,
                mock(LocalFileLongTermMemory.class), mock(MemorySummarizer.class));
        ReflectionTestUtils.setField(service, "idleTtlMinutes", 30L);
        return service;
    }

    @Test
    void 新会话不应被回收() {
        AgentService.AgentSession session = newSession();
        long now = System.currentTimeMillis();

        assertFalse(AgentService.isIdle(session, now, 30 * MINUTE),
                "刚创建的会话不满足空闲条件");
    }

    @Test
    void 超过TTL的空闲会话应被回收() {
        AgentService.AgentSession session = newSession();
        // 模拟会话在 40 分钟前最后活跃
        session.lastAccessAt = System.currentTimeMillis() - 40 * MINUTE;
        long now = System.currentTimeMillis();

        assertTrue(AgentService.isIdle(session, now, 30 * MINUTE),
                "空闲超过 30 分钟的会话应判定为可回收");
    }

    @Test
    void 正在执行的会话即使空闲也不可回收() {
        AgentService.AgentSession session = newSession();
        session.lastAccessAt = System.currentTimeMillis() - 40 * MINUTE;
        session.busy.set(true);
        long now = System.currentTimeMillis();

        assertFalse(AgentService.isIdle(session, now, 30 * MINUTE),
                "busy=true 表示请求正在执行，不允许回收");
    }

    @Test
    void 回收时应中断底层Agent() {
        ReActAgent agent = mock(ReActAgent.class);
        AgentService.AgentSession session = new AgentService.AgentSession(agent, mock(ModelRouter.class), "deepseek-v4-flash");
        session.lastAccessAt = System.currentTimeMillis() - 40 * MINUTE;

        AgentService service = newService();
        service.sessions.put("test-session", session);

        service.evictIdleSessions();

        verify(agent).interrupt();
        assertFalse(service.sessions.containsKey("test-session"), "回收后应从会话表中移除");
    }

    @Test
    void 正在执行的会话不会被定时清理中断() {
        ReActAgent agent = mock(ReActAgent.class);
        AgentService.AgentSession session = new AgentService.AgentSession(agent, mock(ModelRouter.class), "deepseek-v4-flash");
        session.lastAccessAt = System.currentTimeMillis() - 40 * MINUTE;
        session.busy.set(true);

        AgentService service = newService();
        service.sessions.put("busy-session", session);

        service.evictIdleSessions();

        verify(agent, org.mockito.Mockito.never()).interrupt();
        assertTrue(service.sessions.containsKey("busy-session"), "执行中的会话应保留");
    }
}
