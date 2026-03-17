Add-Type -AssemblyName System.Runtime.WindowsRuntime
$ErrorActionPreference = "Stop"

function AwaitOperation($op, [Type]$resultType) {
  $asTask = [System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
    $_.Name -eq 'AsTask' -and $_.IsGenericMethod -and $_.GetParameters().Count -eq 1
  } | Select-Object -First 1
  $generic = $asTask.MakeGenericMethod($resultType)
  $netTask = $generic.Invoke($null, @($op))
  $netTask.Wait(-1) | Out-Null
  return $netTask.Result
}

$listenerType = [Windows.UI.Notifications.Management.UserNotificationListener, Windows.UI.Notifications, ContentType=WindowsRuntime]
$kindType = [Windows.UI.Notifications.NotificationKinds, Windows.UI.Notifications, ContentType=WindowsRuntime]
$resultType = [System.Collections.Generic.IReadOnlyList[Windows.UI.Notifications.UserNotification]]

$notifications = AwaitOperation ($listenerType::Current.GetNotificationsAsync($kindType::Toast)) $resultType
$items = @()

foreach ($n in $notifications) {
  $binding = $n.Notification.Visual.GetBinding('ToastGeneric')
  $texts = @()
  if ($binding) {
    foreach ($t in $binding.GetTextElements()) {
      if ($t.Text) {
        $texts += $t.Text
      }
    }
  }

  $items += [PSCustomObject]@{
    id = [string]$n.Id
    appDisplayName = [string]$n.AppInfo.DisplayInfo.DisplayName
    appId = [string]$n.AppInfo.Id
    createdAt = [string]$n.CreationTime
    texts = @($texts)
  }
}

@($items) | ConvertTo-Json -Depth 5 -Compress
