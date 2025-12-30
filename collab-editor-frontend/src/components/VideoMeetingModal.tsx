import React, { useState, useEffect, useRef } from 'react';
import { videoMeetingApi } from '../api';
import { userApi } from '../api/userApi';
import type { IAgoraRTC } from 'agora-rtc-sdk-ng';
import type { CreateMeetingRequest, MeetingResponse } from '../api';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  Button,
  TextField,
  Typography,
  Box,
  IconButton,
  CircularProgress,
  Alert,
  Paper,
  Tooltip,
  Stack,
  useTheme
} from '@mui/material';
import {
  Close as CloseIcon,
  Mic as MicIcon,
  MicOff as MicOffIcon,
  Videocam as VideocamIcon,
  VideocamOff as VideocamOffIcon,
  ScreenShare as ScreenShareIcon,
  StopScreenShare as StopScreenShareIcon,
  CallEnd as CallEndIcon,
  Add as AddIcon,
  Group as GroupIcon,
  Person as PersonIcon
} from '@mui/icons-material';

interface RemoteUser {
  uid: string | number;
  videoTrack?: any; // IAgoraRTC.IRemoteVideoTrack
  audioTrack?: any; // IAgoraRTC.IRemoteAudioTrack
  hasVideo: boolean;
  hasAudio: boolean;
}

const RemoteVideoPlayer: React.FC<{ user: RemoteUser; username?: string }> = ({ user, username }) => {
  const videoRef = useRef<HTMLDivElement>(null);
  const [displayName, setDisplayName] = useState<string>(username || '');

  useEffect(() => {
    if (username) {
      setDisplayName(username);
    } else {
      // 如果没有传入用户名，尝试从API获取
      const uid = Number(user.uid);
      if (!isNaN(uid) && uid > 0) {
        userApi.getProfile(uid).then(res => {
          if (res.code === 200 && res.data) {
            setDisplayName(res.data.username || res.data.nickname || `用户 ${uid}`);
          }
        }).catch(err => {
          console.error('获取用户信息失败:', err);
        });
      }
    }
  }, [username, user.uid]);

  useEffect(() => {
    if (user.videoTrack && videoRef.current) {
      user.videoTrack.play(videoRef.current);
    }
  }, [user.videoTrack]);

  return (
    <Box sx={{
      flex: '1 0 300px',
      minWidth: '200px',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      m: 1
    }}>
      <Box
        ref={videoRef}
        sx={{
          width: '100%',
          height: 200,
          bgcolor: 'black',
          borderRadius: 2,
          overflow: 'hidden'
        }}
      />
      <Typography variant="subtitle2" sx={{ mt: 1, color: 'text.secondary' }}>
        {displayName || `用户 ${user.uid}`}
      </Typography>
    </Box>
  );
};

const LocalVideoPlayer: React.FC<{ videoTrack: any; username: string }> = ({ videoTrack, username }) => {
  const videoRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (videoTrack && videoRef.current) {
      videoTrack.play(videoRef.current);
    }
    return () => {
      // 组件卸载时不停止轨道，因为轨道由父组件管理，但可以做清理
    };
  }, [videoTrack]);

  return (
    <Box sx={{
      flex: '0 0 300px',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center'
    }}>
      <Box
        ref={videoRef}
        sx={{
          width: '100%',
          height: 200,
          bgcolor: 'black',
          borderRadius: 2,
          overflow: 'hidden',
          position: 'relative'
        }}
      />
      <Typography variant="subtitle2" sx={{ mt: 1, color: 'text.secondary', display: 'flex', alignItems: 'center' }}>
        <PersonIcon fontSize="small" sx={{ mr: 0.5 }} /> {username} (我)
      </Typography>
    </Box>
  );
};

interface VideoMeetingModalProps {
  isVisible: boolean;
  onClose: () => void;
  docId: number;
  currentUserId: number;
  onlineUsers?: { userId: number; username: string }[];
}

/**
 * 视频会议模态框组件
 * 
 * 负责处理视频会议的创建、加入、媒体流管理以及UI展示。
 * 集成了Agora RTC SDK v4。
 */
const VideoMeetingModal: React.FC<VideoMeetingModalProps> = ({
  isVisible,
  onClose,
  docId,
  currentUserId,
  onlineUsers = []
}) => {
  const theme = useTheme();
  // 步骤状态：创建 -> 加入 -> 房间
  const [step, setStep] = useState<'create' | 'join' | 'room'>('create');
  const [meetingTitle, setMeetingTitle] = useState('');
  const [meetingId, setMeetingId] = useState('');
  const [meetingInfo, setMeetingInfo] = useState<MeetingResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [isMicEnabled, setIsMicEnabled] = useState(true);
  const [isCameraEnabled, setIsCameraEnabled] = useState(true);

  // 检查当前文档是否有活跃的会议
  const checkActiveMeeting = async () => {
    try {
      const meeting = await videoMeetingApi.getActiveMeeting(docId);
      if (meeting) {
        setMeetingInfo(meeting);
        setMeetingId(meeting.meetingId); // 自动填充会议ID
        setStep('join');
      } else {
        setStep('create');
      }
    } catch (error) {
      console.error('获取活跃会议失败:', error);
    }
  };

  useEffect(() => {
    if (isVisible) {
      checkActiveMeeting();
    }
  }, [isVisible, docId]);

  // Agora相关引用
  const agoraRef = useRef<IAgoraRTC | null>(null);
  const localStreamRef = useRef<any>(null);
  const [localTracks, setLocalTracks] = useState<any[]>([]); // 添加 state 以触发重渲染
  // 使用 state 管理远程用户，以便 React 正确渲染
  const [remoteUsers, setRemoteUsers] = useState<RemoteUser[]>([]);
  const clientRef = useRef<any>(null);

  /**
   * 初始化 Agora 客户端
   * 
   * 加载 SDK，创建客户端实例，并设置事件监听器。
   */
  const initAgora = async () => {
    try {
      // 动态导入Agora SDK
      const AgoraRTC = await import('agora-rtc-sdk-ng');
      agoraRef.current = AgoraRTC.default;

      // 创建客户端
      // 显式设置区域为中国，使用字符串 'CN' 避免 TS 类型错误
      // 使用 as any 绕过 ClientConfig 类型定义缺失 areaCode 的问题
      const client = agoraRef.current.createClient({
        mode: 'rtc',
        codec: 'vp8',
        areaCode: ['CN'],
      } as any);

      clientRef.current = client;

      // 设置事件监听
      client.on('user-joined', handleUserJoined);
      client.on('user-left', handleUserLeft);
      client.on('user-published', handleUserPublished);
      client.on('user-unpublished', handleUserUnpublished);

      return client;
    } catch (error) {
      console.error('初始化Agora失败:', error);
      throw error;
    }
  };

  /**
   * 处理用户加入事件
   * 
   * 当远端用户加入频道时触发，更新远程用户列表。
   */
  const handleUserJoined = (user: any) => {
    setRemoteUsers(prev => {
      if (prev.find(u => u.uid === user.uid)) return prev;
      return [...prev, { uid: user.uid, hasVideo: false, hasAudio: false }];
    });
  };

  /**
   * 处理用户离开事件
   * 
   * 当远端用户离开频道时触发，停止播放并从列表中移除。
   */
  const handleUserLeft = (user: any) => {
    // 停止播放
    if (user.audioTrack) user.audioTrack.stop();
    if (user.videoTrack) user.videoTrack.stop();
    setRemoteUsers(prev => prev.filter(u => u.uid !== user.uid));
  };

  /**
   * 处理用户发布流事件
   * 
   * 当远端用户发布音频或视频流时触发，订阅流并更新状态。
   */
  const handleUserPublished = async (user: any, mediaType: string) => {
    const client = clientRef.current;
    if (client) {
      await client.subscribe(user, mediaType);

      if (mediaType === 'audio') {
        user.audioTrack?.play();
      }

      setRemoteUsers(prev => {
        const existing = prev.find(u => u.uid === user.uid);
        if (existing) {
          return prev.map(u => u.uid === user.uid ? {
            ...u,
            [mediaType === 'video' ? 'videoTrack' : 'audioTrack']: user[mediaType === 'video' ? 'videoTrack' : 'audioTrack'],
            [mediaType === 'video' ? 'hasVideo' : 'hasAudio']: true
          } : u);
        } else {
          return [...prev, {
            uid: user.uid,
            videoTrack: mediaType === 'video' ? user.videoTrack : undefined,
            audioTrack: mediaType === 'audio' ? user.audioTrack : undefined,
            hasVideo: mediaType === 'video',
            hasAudio: mediaType === 'audio'
          }];
        }
      });
    }
  };

  /**
   * 处理用户取消发布流事件
   * 
   * 当远端用户停止发送流时触发，停止播放并更新状态。
   */
  const handleUserUnpublished = (user: any, mediaType: string) => {
    if (mediaType === 'audio') {
      user.audioTrack?.stop();
    }
    if (mediaType === 'video') {
      user.videoTrack?.stop();
    }
    setRemoteUsers(prev => prev.map(u => u.uid === user.uid ? {
      ...u,
      [mediaType === 'video' ? 'hasVideo' : 'hasAudio']: false
    } : u));
  };

  /**
   * 创建本地音视频流
   * 
   * 尝试获取麦克风和摄像头权限，创建本地轨道。
   * 如果摄像头被占用，会自动降级为仅音频模式。
   */
  const createLocalStream = async () => {
    const AgoraRTC = agoraRef.current;
    if (!AgoraRTC) return null;

    // 确保旧的流被清理
    if (localStreamRef.current) {
      if (Array.isArray(localStreamRef.current)) {
        localStreamRef.current.forEach((track: any) => track?.close?.());
      } else if (localStreamRef.current.close) {
        localStreamRef.current.close();
      }
      localStreamRef.current = null;
    }

    try {
      // Agora Web SDK v4 使用 createMicrophoneAndCameraTracks 代替 createStream
      // 尝试同时获取音频和视频
      const [microphoneTrack, cameraTrack] = await AgoraRTC.createMicrophoneAndCameraTracks();

      // 根据开关状态控制音视频轨道
      if (!isMicEnabled) {
        microphoneTrack.setEnabled(false);
      }
      if (!isCameraEnabled) {
        cameraTrack.setEnabled(false);
      }

      // 存储轨道
      localStreamRef.current = [microphoneTrack, cameraTrack];
      setLocalTracks([microphoneTrack, cameraTrack]);
      return [microphoneTrack, cameraTrack];
    } catch (error: any) {
      console.error('创建本地流失败:', error);

      // 如果是因为设备被占用 (NotReadableError)，尝试降级为仅音频模式
      if (error.code === 'NOT_READABLE' || error.name === 'NotReadableError' || (error.message && error.message.includes('Device in use'))) {
        console.warn('摄像头可能被占用，尝试仅获取音频...');

        // 尝试关闭已经打开的轨道（如果有）
        if (localStreamRef.current) {
          if (Array.isArray(localStreamRef.current)) {
            localStreamRef.current.forEach((track: any) => track?.close?.());
          }
          localStreamRef.current = null;
        }

        // 不在这里设置 Error，因为这是一种降级策略，不是完全失败

        try {
          const microphoneTrack = await AgoraRTC.createMicrophoneAudioTrack();
          if (!isMicEnabled) {
            microphoneTrack.setEnabled(false);
          }
          localStreamRef.current = [microphoneTrack, null];
          setLocalTracks([microphoneTrack, null]);
          setIsCameraEnabled(false); // 强制更新状态
          // 通知用户
          setError('摄像头被占用，已切换为仅音频模式。请关闭其他使用摄像头的应用。');
          return [microphoneTrack, null];
        } catch (audioError) {
          console.error('获取音频失败:', audioError);
          setError('无法获取麦克风和摄像头，请确保设备未被其他应用占用。');
          throw audioError;
        }
      }

      throw error;
    }
  };

  /**
   * 加入会议
   * 
   * 初始化客户端，获取App ID和Token，加入频道，并发布本地流。
   */
  const joinMeeting = async (meeting: MeetingResponse) => {
    if (!meeting) {
      console.error('加入会议失败: 会议信息为空');
      setError('无法获取会议信息');
      return;
    }

    // 检查必要的字段
    // 在 App ID 模式下，token 可能为空，因此不再强制检查 token
    if (!meeting.meetingId) {
      console.error('加入会议失败: 会议ID缺失', meeting);
      setError('会议信息不完整');
      return;
    }

    try {
      setIsLoading(true);
      setError('');

      // 初始化Agora
      const client = await initAgora();

      // 硬编码 App ID 以排除环境变量问题，优先使用服务端返回的 App ID
      let finalAppId = (meeting as any).appId;

      // 如果服务端没有返回 App ID，使用硬编码的 App ID
      if (!finalAppId) {
        finalAppId = '28cc827006c549dfacd541785bd2284f';
      }

      // 确保 App ID 是字符串并去除空格
      if (typeof finalAppId === 'string') {
        finalAppId = finalAppId.trim();
      }

      if (!finalAppId) {
        console.error('未配置有效的Agora App ID');
        setError('系统配置错误: Agora App ID 缺失');
        setIsLoading(false);
        return;
      }

      // 加入频道 - 注意：将 currentUserId 转换为字符串，以匹配后端生成的 User Account Token
      // 后端使用 buildTokenWithUserAccount 生成 Token，前端必须以字符串形式传入 UID
      // 如果 token 为空字符串（App ID 模式），转换为 null
      // 注意：某些情况下 token 可能是 "null" 字符串或 dummy token，需特殊处理
      let tokenToUse: string | null = meeting.token || null;
      if (!tokenToUse || tokenToUse === 'null' || tokenToUse.startsWith('dummy_token')) {
        tokenToUse = null; // App ID 模式下 token 应为 null
      }

      await client.join(
        finalAppId,
        meeting.channelName,
        tokenToUse,
        currentUserId
      );

      // 创建并发布本地流
      const localStream = await createLocalStream();
      if (localStream) {
        // 过滤掉 null 的轨道 (例如降级为仅音频时，videoTrack 为 null)
        const tracksToPublish = localStream.filter(track => track !== null);
        if (tracksToPublish.length > 0) {
          await client.publish(tracksToPublish);
        }
      }

      setMeetingInfo(meeting);
      setStep('room');
    } catch (error: any) {
      console.error('加入会议失败:', error);
      let errorMsg = '加入会议失败，请重试';

      if (error.code === 'CAN_NOT_GET_GATEWAY_SERVER' || (error.message && error.message.includes('invalid vendor key'))) {
        errorMsg = '加入会议失败：Agora App ID/Token 配置错误。请确认您的 Agora 项目是否启用了 App Certificate。如果是，请在后端配置 agora.certificate。';
      } else if (error.code === 'INVALID_TOKEN') {
        errorMsg = '加入会议失败：无效的 Token。';
      } else if (error.message) {
        errorMsg = `加入会议失败: ${error.message}`;
      }

      setError(errorMsg);
    } finally {
      setIsLoading(false);
    }
  };

  // 创建会议
  const handleCreateMeeting = async () => {
    if (!meetingTitle.trim()) {
      setError('请输入会议标题');
      return;
    }

    try {
      setIsLoading(true);
      setError('');

      const request: CreateMeetingRequest = {
        title: meetingTitle,
        docId,
      };

      const response = await videoMeetingApi.createMeeting(request);
      await joinMeeting(response);
    } catch (error) {
      console.error('创建会议失败:', error);
      setError('创建会议失败，请重试');
    } finally {
      setIsLoading(false);
    }
  };

  // 加入指定ID的会议
  const handleJoinMeeting = async () => {
    if (!meetingId.trim()) {
      setError('请输入会议ID');
      return;
    }

    try {
      setIsLoading(true);
      setError('');

      const response = await videoMeetingApi.getMeetingInfo(meetingId);
      if (!response) {
        setError('未找到该会议，请检查会议ID是否正确');
        return;
      }
      await joinMeeting(response);
    } catch (error) {
      console.error('加入会议失败:', error);
      setError('加入会议失败，请检查会议ID是否正确');
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * 切换麦克风状态
   */
  const toggleMic = async () => {
    const newMicState = !isMicEnabled;
    setIsMicEnabled(newMicState);

    // localStreamRef.current 是数组 [microphoneTrack, cameraTrack]
    const localTracks = localStreamRef.current;
    if (localTracks && localTracks[0]) {
      // 使用 setEnabled 控制音频轨道
      await localTracks[0].setEnabled(newMicState);
    }
  };

  /**
   * 切换摄像头状态
   */
  const toggleCamera = async () => {
    const newCameraState = !isCameraEnabled;
    setIsCameraEnabled(newCameraState);

    // localStreamRef.current 是数组 [microphoneTrack, cameraTrack]
    const localTracks = localStreamRef.current;
    if (localTracks && localTracks[1]) {
      // 使用 setEnabled 控制视频轨道
      await localTracks[1].setEnabled(newCameraState);
    }
  };

  // 屏幕共享相关状态
  const [isScreenSharing, setIsScreenSharing] = useState(false);
  const screenShareStreamRef = useRef<any>(null);

  /**
   * 切换屏幕共享
   * 
   * 开启或关闭屏幕共享流。
   */
  const toggleScreenShare = async () => {
    const client = clientRef.current;
    if (!client) return;

    try {
      if (!isScreenSharing) {
        // 开始屏幕共享
        const AgoraRTC = agoraRef.current;
        if (!AgoraRTC) return;

        // 创建屏幕共享流
        // 改用 'auto' 模式，这是兼容性最好的选项，允许用户在浏览器弹窗中选择是否共享音频
        // 恢复 720p_1 和 detail 模式以获得更好的文档共享体验
        const screenTracksResult = await AgoraRTC.createScreenVideoTrack({
          encoderConfig: '720p_1',
          optimizationMode: 'detail',
        }, 'auto');

        // 处理可能的数组返回 (Video + Audio)
        let tracksToPublish = [];
        if (Array.isArray(screenTracksResult)) {
          tracksToPublish = screenTracksResult;
        } else {
          tracksToPublish = [screenTracksResult];
        }

        // 发布屏幕共享流
        await client.publish(tracksToPublish);
        screenShareStreamRef.current = screenTracksResult;
        setIsScreenSharing(true);
      } else {
        // 停止屏幕共享
        if (screenShareStreamRef.current) {
          // 处理数组或单个轨道
          if (Array.isArray(screenShareStreamRef.current)) {
            const client = clientRef.current;
            if (client) {
              await client.unpublish(screenShareStreamRef.current);
            }
            screenShareStreamRef.current.forEach((track: any) => track.close());
          } else {
            const client = clientRef.current;
            if (client) {
              await client.unpublish(screenShareStreamRef.current);
            }
            screenShareStreamRef.current.close();
          }
          screenShareStreamRef.current = null;
          setIsScreenSharing(false);
        }
      }
    } catch (error) {
      console.error('屏幕共享操作失败:', error);
      setError('屏幕共享操作失败，请重试');
    }
  };

  // 结束会议
  const handleEndMeeting = async () => {
    try {
      setIsLoading(true);
      setError('');

      if (meetingInfo) {
        await videoMeetingApi.endMeeting(meetingInfo.meetingId);
      }

      // 清理Agora资源
      await cleanupAgora();

      // 关闭弹窗
      onClose();
    } catch (error) {
      console.error('结束会议失败:', error);
      setError('结束会议失败，请重试');
    } finally {
      setIsLoading(false);
    }
  };

  // 离开会议
  const handleLeaveMeeting = async () => {
    try {
      // 清理Agora资源
      await cleanupAgora();

      // 关闭弹窗
      onClose();
    } catch (error) {
      console.error('离开会议失败:', error);
      setError('离开会议失败，请重试');
    }
  };

  /**
   * 清理 Agora 资源
   * 
   * 停止所有本地轨道，取消发布，离开频道，并清空远程用户列表。
   */
  const cleanupAgora = async () => {
    // 停止本地流
    if (localStreamRef.current) {
      if (Array.isArray(localStreamRef.current)) {
        localStreamRef.current.forEach((track: any) => {
          if (track && typeof track.close === 'function') {
            track.close();
          }
        });
      } else if (typeof localStreamRef.current.close === 'function') {
        // Fallback in case it's a single stream object (older SDK versions or different implementation)
        localStreamRef.current.close();
      }
      localStreamRef.current = null;
      setLocalTracks([]);
    }

    // 停止屏幕共享流
    if (screenShareStreamRef.current) {
      if (Array.isArray(screenShareStreamRef.current)) {
        screenShareStreamRef.current.forEach((track: any) => {
          if (track && typeof track.close === 'function') {
            track.close();
          }
        });
      } else if (typeof screenShareStreamRef.current.close === 'function') {
        screenShareStreamRef.current.close();
      }
      screenShareStreamRef.current = null;
      setIsScreenSharing(false);
    }

    // 离开频道
    if (clientRef.current) {
      await clientRef.current.leave();
      clientRef.current = null;
    }

    // 清理远程流
    setRemoteUsers([]);
  };

  // 组件卸载或弹窗关闭时清理资源
  useEffect(() => {
    return () => {
      // 无论当前状态如何，组件卸载时都必须清理资源
      cleanupAgora();
    };
  }, []);

  if (!isVisible) return null;

  return (
    <Dialog
      open={isVisible}
      onClose={step === 'room' ? handleLeaveMeeting : onClose}
      maxWidth="lg"
      fullWidth
      PaperProps={{
        sx: {
          height: '80vh',
          display: 'flex',
          flexDirection: 'column',
          bgcolor: 'background.paper',
          // backgroundImage: 'linear-gradient(to bottom, #ffffff, #f8f9fa)'
        }
      }}
    >
      <DialogTitle sx={{
        m: 0,
        p: 2,
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        borderBottom: `1px solid ${theme.palette.divider}`
      }}>
        <Typography variant="h6" component="div" sx={{ fontWeight: 'bold', display: 'flex', alignItems: 'center' }}>
          {step === 'create' && <><AddIcon sx={{ mr: 1 }} /> 创建视频会议</>}
          {step === 'join' && <><GroupIcon sx={{ mr: 1 }} /> 加入视频会议</>}
          {step === 'room' && <><VideocamIcon sx={{ mr: 1 }} /> {meetingInfo?.title}</>}
        </Typography>
        <IconButton
          aria-label="close"
          onClick={step === 'room' ? handleLeaveMeeting : onClose}
          sx={{
            color: (theme) => theme.palette.grey[500],
          }}
        >
          <CloseIcon />
        </IconButton>
      </DialogTitle>

      <DialogContent sx={{ flex: 1, p: 3, display: 'flex', flexDirection: 'column', overflowY: 'auto' }}>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        {step === 'create' && (
          <Box sx={{ maxWidth: 500, mx: 'auto', mt: 4, width: '100%' }}>
            <Typography variant="body1" gutterBottom>
              请输入会议标题以开始新的视频会议
            </Typography>
            <TextField
              autoFocus
              margin="dense"
              id="meeting-title"
              label="会议标题"
              type="text"
              fullWidth
              variant="outlined"
              value={meetingTitle}
              onChange={(e) => setMeetingTitle(e.target.value)}
              sx={{ mb: 3 }}
            />
            <Stack direction="row" spacing={2} justifyContent="flex-end">
              <Button onClick={onClose} variant="outlined" color="inherit">
                取消
              </Button>
              <Button
                onClick={handleCreateMeeting}
                variant="contained"
                disabled={isLoading}
                startIcon={isLoading ? <CircularProgress size={20} /> : <AddIcon />}
              >
                {isLoading ? '创建中...' : '创建会议'}
              </Button>
            </Stack>
            <Box sx={{ mt: 3, textAlign: 'center' }}>
              <Button color="primary" onClick={() => setStep('join')}>
                已有会议？加入会议
              </Button>
            </Box>
          </Box>
        )}

        {step === 'join' && (
          <Box sx={{ maxWidth: 500, mx: 'auto', mt: 4, width: '100%' }}>
            {meetingInfo ? (
              <Paper elevation={3} sx={{ p: 3, textAlign: 'center' }}>
                <Typography variant="h6" gutterBottom color="primary">
                  发现正在进行的会议
                </Typography>
                <Typography variant="h5" sx={{ my: 2, fontWeight: 'bold' }}>
                  {meetingInfo.title}
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                  会议ID: {meetingInfo.meetingId}
                </Typography>
                <Stack direction="row" spacing={2} justifyContent="center">
                  <Button onClick={onClose} variant="outlined" color="inherit">
                    暂不加入
                  </Button>
                  <Button
                    onClick={() => joinMeeting(meetingInfo)}
                    variant="contained"
                    disabled={isLoading}
                    startIcon={isLoading ? <CircularProgress size={20} /> : <GroupIcon />}
                  >
                    {isLoading ? '加入中...' : '立即加入'}
                  </Button>
                </Stack>
              </Paper>
            ) : (
              <>
                <Typography variant="body1" gutterBottom>
                  请输入会议ID以加入现有会议
                </Typography>
                <TextField
                  autoFocus
                  margin="dense"
                  id="meeting-id"
                  label="会议ID"
                  type="text"
                  fullWidth
                  variant="outlined"
                  value={meetingId}
                  onChange={(e) => setMeetingId(e.target.value)}
                  sx={{ mb: 3 }}
                />
                <Stack direction="row" spacing={2} justifyContent="flex-end">
                  <Button onClick={onClose} variant="outlined" color="inherit">
                    取消
                  </Button>
                  <Button
                    onClick={handleJoinMeeting}
                    variant="contained"
                    disabled={isLoading}
                    startIcon={isLoading ? <CircularProgress size={20} /> : <GroupIcon />}
                  >
                    {isLoading ? '加入中...' : '加入会议'}
                  </Button>
                </Stack>
              </>
            )}

            <Box sx={{ mt: 3, textAlign: 'center' }}>
              <Button color="primary" onClick={() => {
                setMeetingInfo(null);
                setStep('create');
              }}>
                返回创建会议
              </Button>
            </Box>
          </Box>
        )}

        {step === 'room' && (
          <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
            <Box sx={{ flex: 1, display: 'flex', gap: 2, mb: 2, overflowY: 'auto' }}>
              <LocalVideoPlayer
                videoTrack={localTracks?.[1]}
                username={onlineUsers?.find(u => u.userId === currentUserId)?.username || localStorage.getItem('username') || '我'}
              />

              <Box
                id="remote-videos"
                sx={{
                  flex: 1,
                  display: 'flex',
                  flexWrap: 'wrap',
                  gap: 2,
                  alignContent: 'flex-start'
                }}
              >
                {remoteUsers.map(user => {
                  const remoteUser = onlineUsers.find(u => u.userId === Number(user.uid));
                  return (
                    <RemoteVideoPlayer
                      key={user.uid}
                      user={user}
                      username={remoteUser?.username}
                    />
                  );
                })}
              </Box>
            </Box>

            <Paper
              elevation={3}
              sx={{
                p: 2,
                display: 'flex',
                justifyContent: 'center',
                gap: 3,
                bgcolor: 'background.default',
                borderRadius: 4
              }}
            >
              <Tooltip title={isMicEnabled ? '关闭麦克风' : '打开麦克风'}>
                <IconButton
                  onClick={toggleMic}
                  color={isMicEnabled ? 'primary' : 'error'}
                  sx={{
                    border: '1px solid',
                    borderColor: isMicEnabled ? 'primary.main' : 'error.main',
                    width: 50, height: 50
                  }}
                >
                  {isMicEnabled ? <MicIcon /> : <MicOffIcon />}
                </IconButton>
              </Tooltip>

              <Tooltip title={isCameraEnabled ? '关闭摄像头' : '打开摄像头'}>
                <IconButton
                  onClick={toggleCamera}
                  color={isCameraEnabled ? 'primary' : 'error'}
                  sx={{
                    border: '1px solid',
                    borderColor: isCameraEnabled ? 'primary.main' : 'error.main',
                    width: 50, height: 50
                  }}
                >
                  {isCameraEnabled ? <VideocamIcon /> : <VideocamOffIcon />}
                </IconButton>
              </Tooltip>

              <Tooltip title={isScreenSharing ? '停止屏幕共享' : '开始屏幕共享'}>
                <IconButton
                  onClick={toggleScreenShare}
                  color={isScreenSharing ? 'success' : 'default'}
                  sx={{
                    border: '1px solid',
                    borderColor: isScreenSharing ? 'success.main' : 'grey.400',
                    width: 50, height: 50
                  }}
                >
                  {isScreenSharing ? <StopScreenShareIcon /> : <ScreenShareIcon />}
                </IconButton>
              </Tooltip>

              <Tooltip title="结束会议">
                <IconButton
                  onClick={handleEndMeeting}
                  color="error"
                  disabled={isLoading}
                  sx={{
                    border: '1px solid',
                    borderColor: 'error.main',
                    bgcolor: 'error.main',
                    color: 'white',
                    width: 50, height: 50,
                    '&:hover': {
                      bgcolor: 'error.dark',
                    }
                  }}
                >
                  <CallEndIcon />
                </IconButton>
              </Tooltip>
            </Paper>
          </Box>
        )}
      </DialogContent>
    </Dialog>
  );
};

export default VideoMeetingModal;
