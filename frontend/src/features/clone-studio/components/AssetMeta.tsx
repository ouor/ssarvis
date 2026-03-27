type AssetMetaProps = {
  label: string
  ownerDisplayName?: string | null
  fallbackOwnerDisplayName: string
  publicBadge?: boolean
  action?: {
    label: string
    disabled?: boolean
    onClick: () => void | Promise<void>
  }
}

function AssetMeta({ label, ownerDisplayName, fallbackOwnerDisplayName, publicBadge = false, action }: AssetMetaProps) {
  return (
    <div className="asset-meta-row">
      <span className={`asset-badge${publicBadge ? ' asset-badge-public' : ''}`}>{label}</span>
      <span className="asset-owner">작성자 {ownerDisplayName ?? fallbackOwnerDisplayName}</span>
      {action ? (
        <button
          className="secondary-button"
          disabled={action.disabled}
          onClick={(event) => {
            event.preventDefault()
            void action.onClick()
          }}
          type="button"
        >
          {action.label}
        </button>
      ) : null}
    </div>
  )
}

export default AssetMeta
