import { expect, test, type Page, type Route } from '@playwright/test'

type ApiEnvelope<T> = {
  code: number
  message: string
  data: T
}

async function mockApi<T>(route: Route, data: T, status = 200) {
  const body: ApiEnvelope<T> = {
    code: 0,
    message: 'ok',
    data
  }

  await route.fulfill({
    status,
    contentType: 'application/json',
    body: JSON.stringify(body)
  })
}

async function mockLoginSuccess(page: Page) {
  await page.route('**/client/user/login', async (route) => {
    await mockApi(route, {
      token: 'token-e2e',
      refreshToken: 'refresh-e2e',
      expireIn: 3600,
      deviceId: 'device-e2e',
      user: {
        id: 1,
        username: 'tester',
        email: 'tester@example.com',
        avatarUrl: null,
        phone: null,
        status: 1,
        createdAt: new Date().toISOString()
      }
    })
  })
}

async function loginFrom(page: Page, fromPath: string) {
  await mockLoginSuccess(page)
  await page.goto(fromPath)
  await page.getByLabel('邮箱').fill('tester@example.com')
  await page.getByLabel('密码').fill('secret')
  await page.getByRole('button', { name: '登录' }).click()
}

test.beforeEach(async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
})

test('登录成功后跳转，受保护路由未登录时重定向到登录页', async ({ page }) => {
  await loginFrom(page, '/dashboard')

  await expect(page).toHaveURL(/\/dashboard$/)
  await expect(page.getByRole('heading', { name: '工作台' })).toBeVisible()
})

test('dashboard 点击新建 Agent 跳转到 agents 页面', async ({ page }) => {
  await loginFrom(page, '/dashboard')

  await page.getByRole('button', { name: '新建 Agent' }).click()
  await expect(page).toHaveURL(/\/agents$/)
})

test('agents 创建后进入 workflow 路由', async ({ page }) => {
  await page.route('**/api/agent/list**', async (route) => {
    await mockApi(route, [])
  })

  await page.route('**/api/agent/create', async (route) => {
    await mockApi(route, 101)
  })

  await page.route('**/api/agent/101', async (route) => {
    await mockApi(route, {
      id: 101,
      name: '未命名 Agent',
      version: 1,
      status: 1,
      graphJson: JSON.stringify({
        version: '1.0.0',
        nodes: [
          { nodeId: 'start', nodeName: '开始节点', nodeType: 'START' },
          { nodeId: 'end', nodeName: '结束节点', nodeType: 'END' }
        ],
        edges: []
      })
    })
  })

  await loginFrom(page, '/agents')

  await page.getByRole('button', { name: '新建 Agent' }).click()
  await expect(page).toHaveURL(/\/agents\/101\/workflow$/)
  await expect(page.getByRole('heading', { name: 'Workflow 编辑' })).toBeVisible()
})

test('workflow 保存失败时可见错误反馈', async ({ page }) => {
  await page.route('**/api/agent/101', async (route) => {
    await mockApi(route, {
      id: 101,
      name: '未命名 Agent',
      version: 1,
      status: 1,
      graphJson: JSON.stringify({
        version: '1.0.0',
        nodes: [
          { nodeId: 'start', nodeName: '开始节点', nodeType: 'START' },
          { nodeId: 'end', nodeName: '结束节点', nodeType: 'END' }
        ],
        edges: []
      })
    })
  })

  await page.route('**/api/agent/update', async (route) => {
    await route.fulfill({
      status: 500,
      contentType: 'application/json',
      body: JSON.stringify({ code: 500, message: 'save failed', data: null })
    })
  })

  await loginFrom(page, '/agents/101/workflow')

  await page.getByRole('button', { name: '添加连线' }).click()
  await page.getByRole('button', { name: '保存' }).click()

  await expect(page.getByText('保存失败，请稍后重试')).toBeVisible()
})

test('chat 流式响应与手动中断关键路径', async ({ page }) => {
  await page.route('**/api/chat/conversations**', async (route) => {
    const method = route.request().method().toUpperCase()

    if (method === 'POST') {
      await mockApi(route, 'conv-1')
      return
    }

    await mockApi(route, {
      total: 1,
      pages: 1,
      list: [
        {
          id: 'conv-1',
          userId: '1',
          agentId: '1',
          title: '会话1',
          createdAt: '2026-01-01T00:00:00Z',
          updatedAt: '2026-01-01T00:00:00Z'
        }
      ]
    })
  })

  await page.route('**/api/chat/conversations/conv-1/messages**', async (route) => {
    await mockApi(route, [])
  })

  let streamCallCount = 0
  await page.route('**/api/workflow/execution/start', async (route) => {
    streamCallCount += 1

    if (streamCallCount === 1) {
      await route.fulfill({
        status: 200,
        contentType: 'text/event-stream',
        body: [
          'event: connected',
          'data: {"executionId":"exec-1"}',
          '',
          'event: update',
          'data: {"delta":"你好"}',
          '',
          'event: finish',
          'data: {}',
          ''
        ].join('\n')
      })
      return
    }

    await new Promise((resolve) => setTimeout(resolve, 1200))
    await route.fulfill({
      status: 200,
      contentType: 'text/event-stream',
      body: [
        'event: connected',
        'data: {"executionId":"exec-2"}',
        ''
      ].join('\n')
    })
  })

  await page.route('**/api/workflow/execution/stop', async (route) => {
    await mockApi(route, null)
  })

  await loginFrom(page, '/chat')

  await page.getByRole('button', { name: /会话1/ }).click()

  await page.getByPlaceholder('输入消息后发送').fill('第一条消息')
  await expect(page.getByRole('button', { name: '发送' })).toBeEnabled()
  await page.getByRole('button', { name: '发送' }).click()
  await expect(page.getByText('第一条消息')).toBeVisible()
  await expect(page.getByText('COMPLETED')).toHaveCount(2)

  await page.getByPlaceholder('输入消息后发送').fill('第二条消息')
  await expect(page.getByRole('button', { name: '发送' })).toBeEnabled()
  await page.getByRole('button', { name: '发送' }).click()
  await page.getByRole('button', { name: '中断' }).click()

  await expect(page.getByText('已手动中断')).toBeVisible()
})
