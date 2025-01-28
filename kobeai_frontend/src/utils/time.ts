import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import relativeTime from 'dayjs/plugin/relativeTime'
import calendar from 'dayjs/plugin/calendar'
import customParseFormat from 'dayjs/plugin/customParseFormat'
import utc from 'dayjs/plugin/utc'
import timezone from 'dayjs/plugin/timezone'

dayjs.extend(relativeTime)
dayjs.extend(calendar)
dayjs.extend(customParseFormat)
dayjs.extend(utc)
dayjs.extend(timezone)
dayjs.locale('zh-cn')

// 辅助函数：尝试多种方式解析日期
function parseDate(time: string | number | Date): dayjs.Dayjs | null {
  if (!time) return null

  // 如果是Date对象
  if (time instanceof Date) {
    return dayjs(time)
  }

  // 如果是时间戳（数字或字符串形式）
  if (typeof time === 'number' || !isNaN(Number(time))) {
    return dayjs(Number(time))
  }

  // 尝试直接解析
  let date = dayjs(time)
  if (date.isValid()) return date

  // 尝试特定格式
  const formats = [
    'YYYY-MM-DD HH:mm:ss',
    'YYYY/MM/DD HH:mm:ss',
    'YYYY-MM-DDTHH:mm:ss',
    'YYYY-MM-DDTHH:mm:ss.SSS[Z]',
    'YYYY-MM-DD'
  ]

  for (const format of formats) {
    date = dayjs(time, format)
    if (date.isValid()) return date
  }

  return null
}

export function formatTime(time: string | number | Date): string {
  const date = parseDate(time)
  if (!date) return ''
  
  const now = dayjs()
  
  // 如果是今天的消息
  if (date.isSame(now, 'day')) {
    return date.format('HH:mm')
  }
  
  // 如果是昨天的消息
  if (date.isSame(now.subtract(1, 'day'), 'day')) {
    return `昨天 ${date.format('HH:mm')}`
  }
  
  // 如果是本周的消息
  if (date.isSame(now, 'week')) {
    return date.format('dddd HH:mm')
  }
  
  // 如果是今年的消息
  if (date.isSame(now, 'year')) {
    return date.format('M月D日 HH:mm')
  }
  
  // 其他情况显示完整日期
  return date.format('YYYY年M月D日 HH:mm')
}

export function formatRegistrationTime(time: string | number | Date): string {
  const date = parseDate(time)
  if (!date) {
    // 如果所有解析方法都失败了，返回未知
    console.debug('Unable to parse date:', time)
    return '未知'
  }
  
  // 显示完整的注册时间
  return date.format('YYYY年MM月DD日 HH:mm:ss')
} 