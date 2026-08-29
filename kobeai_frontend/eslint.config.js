import pluginVue from 'eslint-plugin-vue'
import { defineConfigWithVueTs, vueTsConfigs } from '@vue/eslint-config-typescript'
import skipFormatting from '@vue/eslint-config-prettier/skip-formatting'

export default defineConfigWithVueTs(
  {
    name: 'app/files-to-lint',
    files: ['**/*.{ts,mts,tsx,vue}']
  },
  {
    name: 'app/ignores',
    ignores: [
      'dist/**',
      'node_modules/**',
      // 自动生成的类型声明文件不参与检查
      'src/auto-imports.d.ts',
      'src/components.d.ts',
      // 防止 tsc 误发射的编译产物干扰
      'src/**/*.js'
    ]
  },
  pluginVue.configs['flat/essential'],
  vueTsConfigs.recommended,
  skipFormatting,
  {
    name: 'app/custom-rules',
    rules: {
      // 历史页面组件均为单词命名（Home/Login/Chat 等），重命名涉及路由与引用面太广，关闭该规则
      'vue/multi-word-component-names': 'off',
      // 项目现状存在大量 any（渐进迁移），先降级为警告
      '@typescript-eslint/no-explicit-any': 'warn',
      // 未使用变量报错，但允许 _ 前缀的占位参数
      '@typescript-eslint/no-unused-vars': [
        'error',
        { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }
      ]
    }
  }
)
