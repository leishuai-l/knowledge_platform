import { App } from 'vue'

// 按需导入常用图标
import {
  User,
  Lock,
  Document,
  Upload,
  Download,
  Search,
  Bell,
  Setting,
  House,
  Menu,
  Close,
  Edit,
  Delete,
  Plus,
  Check,
  Warning,
  InfoFilled,
  SuccessFilled,
  CircleClose,
  View,
  Star,
  Message,
  Share,
  Refresh,
  ArrowLeft,
  ArrowRight,
  Calendar,
  Clock,
  Location,
  Phone,
  Link,
  Picture,
  VideoPlay,
  Files,
  Folder,
  FolderOpened,
  Connection,
  Notification
} from '@element-plus/icons-vue'

// 图标映射表
const icons = {
  User,
  Lock,
  Document,
  Upload,
  Download,
  Search,
  Bell,
  Setting,
  House,
  Menu,
  Close,
  Edit,
  Delete,
  Plus,
  Check,
  Warning,
  InfoFilled,
  SuccessFilled,
  CircleClose,
  View,
  Star,
  Message,
  Share,
  Refresh,
  ArrowLeft,
  ArrowRight,
  Calendar,
  Clock,
  Location,
  Phone,
  Link,
  Picture,
  VideoPlay,
  Files,
  Folder,
  FolderOpened,
  Connection,
  Notification
}

// 注册图标插件
export function installIcons(app: App) {
  Object.entries(icons).forEach(([key, component]) => {
    app.component(key, component)
  })
}

export default icons