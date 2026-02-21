function ForgotPasswordPage() {
  return (
    <main className="min-h-screen bg-background text-foreground">
      <section className="mx-auto max-w-md px-6 py-20">
        <h1 className="text-2xl font-semibold">忘记密码</h1>
        <button type="button" className="mt-6 rounded bg-primary px-4 py-2 text-sm text-primary-foreground">
          发送重置邮件
        </button>
      </section>
    </main>
  )
}

export default ForgotPasswordPage
