import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import relativeTime from 'dayjs/plugin/relativeTime'
import calendar from 'dayjs/plugin/calendar'

dayjs.extend(relativeTime)
dayjs.extend(calendar)
dayjs.locale('zh-cn')

export function formatTime(time: string | number | Date): string {
  if (!time) return ''
  
  const date = dayjs(time)
  const now = dayjs()
  
  // 如果时间无效，返回空字符串
  if (!date.isValid()) {
    console.warn('Invalid date:', time)
    return ''
  }
  
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