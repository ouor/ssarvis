import type { CurrentUser } from '../features/clone-studio/types'
import CloneGridSection from '../features/clone-studio/components/CloneGridSection'
import FriendPanel from '../features/clone-studio/components/FriendPanel'
import LiveSessionPanel from '../features/clone-studio/components/LiveSessionPanel'
import StudioHero from '../features/clone-studio/components/StudioHero'
import StudioTabs from '../features/clone-studio/components/StudioTabs'
import CloneActionsModal from '../features/clone-studio/components/modals/CloneActionsModal'
import CreateCloneModal from '../features/clone-studio/components/modals/CreateCloneModal'
import DebateSetupModal from '../features/clone-studio/components/modals/DebateSetupModal'
import VoicePickerModal from '../features/clone-studio/components/modals/VoicePickerModal'
import { useCloneStudio } from '../features/clone-studio/hooks/useCloneStudio'
import SnsShell from '../features/sns-shell/SnsShell'
import '../features/clone-studio/styles/layout.css'
import '../features/clone-studio/styles/panels.css'
import '../features/clone-studio/styles/modals.css'

type CloneStudioPageProps = {
  currentUser: CurrentUser
  deactivating: boolean
  onDeactivate: () => Promise<void>
  onLogout: () => void
}

function CloneStudioPage({ currentUser, deactivating, onDeactivate, onLogout }: CloneStudioPageProps) {
  const studio = useCloneStudio(currentUser)
  const selectedClone = studio.selectedClone
  const managedClone = studio.mineClones[0] ?? null
  const managedVoice = studio.mineVoices[0] ?? null

  const profileWorkspace = (
    <>
      <section className="sns-shell-persona-panel">
        <header className="sns-shell-post-header">
          <div>
            <strong>내 AI 프로필 자산</strong>
            <p>이 단계부터 클론과 보이스는 독립 캐릭터가 아니라 내 계정을 대리하는 1:1 자산으로 다룹니다.</p>
          </div>
          <button className="secondary-button" onClick={() => studio.setActiveTab('clones')} type="button">
            프로필 작업공간 열기
          </button>
        </header>

        <div className="sns-shell-persona-grid">
          <article className="sns-shell-persona-card">
            <p className="sns-shell-kicker">Clone</p>
            <strong>{managedClone ? managedClone.alias : '아직 클론이 없습니다.'}</strong>
            <p>{managedClone ? managedClone.shortDescription : '설문 기반으로 내 프로필을 대리할 클론을 먼저 만들어야 합니다.'}</p>
            <span>{managedClone ? '사용자당 1개만 유지되며, 새로 만들면 기존 자산을 갱신합니다.' : '프로필 작업공간에서 생성할 수 있습니다.'}</span>
          </article>

          <article className="sns-shell-persona-card">
            <p className="sns-shell-kicker">Voice</p>
            <strong>{managedVoice ? managedVoice.displayName : '아직 보이스가 없습니다.'}</strong>
            <p>{managedVoice ? `${managedVoice.voiceId} · ${managedVoice.originalFilename}` : '클론 음성 응답에 사용할 대표 보이스를 하나 등록할 수 있습니다.'}</p>
            <span>{managedVoice ? '사용자당 1개만 유지되며, 새 등록은 내 대표 보이스를 교체합니다.' : '프로필 작업공간에서 등록할 수 있습니다.'}</span>
          </article>
        </div>
      </section>

      <StudioHero
        activeTab={studio.activeTab}
        cloneCount={studio.clones.length}
        currentUser={currentUser}
        friendCount={studio.friends.length}
        voiceCount={studio.voices.length}
      />
      <StudioTabs activeTab={studio.activeTab} onTabChange={studio.setActiveTab} />

      {studio.activeTab === 'clones' ? (
        <CloneGridSection
          currentUser={currentUser}
          friendClones={studio.friendClones}
          loadError={studio.cloneLoadError}
          mineClones={studio.mineClones}
          onCloneSelect={studio.openCloneActions}
          onCreateClone={studio.openCreateCloneModal}
          publicClones={studio.publicClones}
        />
      ) : studio.activeTab === 'friends' ? (
        <FriendPanel
          friendActionKey={studio.friendActionKey}
          friendLoadError={studio.friendLoadError}
          friendSearchError={studio.friendSearchError}
          friendSearchLoading={studio.friendSearchLoading}
          friendSearchQuery={studio.friendSearchQuery}
          friendSearchResults={studio.friendSearchResults}
          friends={studio.friends}
          onAcceptRequest={studio.acceptFriendRequest}
          onCancelRequest={studio.cancelFriendRequest}
          onRejectRequest={studio.rejectFriendRequest}
          onSearchQueryChange={studio.setFriendSearchQuery}
          onSearchSubmit={studio.handleFriendSearchSubmit}
          onSendRequest={studio.sendFriendRequest}
          onUnfriend={studio.unfriend}
          receivedRequests={studio.receivedFriendRequests}
          sentRequests={studio.sentFriendRequests}
        />
      ) : (
        <LiveSessionPanel
          chatHistory={studio.chatHistory}
          chatHistoryLoadError={studio.chatHistoryLoadError}
          currentUser={currentUser}
          debateHistory={studio.debateHistory}
          debateHistoryLoadError={studio.debateHistoryLoadError}
          liveChat={studio.liveChat}
          liveDebate={studio.liveDebate}
          onChatInputChange={studio.handleChatInputChange}
          onOpenChatHistory={studio.openChatHistorySession}
          onOpenDebateHistory={studio.openDebateHistorySession}
          onChatSpeechToggle={studio.handleChatSpeechToggle}
          onChatSubmit={studio.handleChatSubmit}
          onDebateExit={studio.handleDebateExit}
          onShowClones={() => studio.setActiveTab('clones')}
        />
      )}

      {studio.modalState?.type === 'create-clone' ? (
        <CreateCloneModal
          answers={studio.answers}
          answeredCount={studio.answeredCount}
          canCreateClone={studio.canCreateClone}
          createCloneError={studio.createCloneError}
          creatingClone={studio.creatingClone}
          loadingQuestions={studio.loadingQuestions}
          onAnswerChange={studio.handleQuestionAnswer}
          onClose={studio.closeModal}
          onSubmit={studio.handleCloneCreate}
          questionError={studio.questionError}
          questions={studio.questions}
        />
      ) : null}

      {studio.modalState?.type === 'clone-actions' && selectedClone ? (
        <CloneActionsModal
          canManageVisibility={studio.mineClones.some((clone) => clone.cloneId === selectedClone.cloneId)}
          clone={selectedClone}
          currentUserDisplayName={currentUser.displayName}
          onChatSelect={studio.openVoicePicker}
          onClose={studio.closeModal}
          onDebateSelect={studio.openDebateSetup}
          onToggleVisibility={() => studio.toggleCloneVisibility(selectedClone)}
          visibilityUpdating={studio.cloneVisibilityUpdatingId === selectedClone.cloneId}
        />
      ) : null}

      {studio.modalState?.type === 'voice-picker' && selectedClone ? (
        <VoicePickerModal
          clone={selectedClone}
          currentUserDisplayName={currentUser.displayName}
          friendVoices={studio.friendVoices}
          mineVoices={studio.mineVoices}
          onBack={studio.goBackToCloneActions}
          onClose={studio.closeModal}
          onStartChat={studio.handleStartChat}
          onToggleVoiceVisibility={studio.toggleVoiceVisibility}
          onVoiceAliasChange={studio.setVoiceAlias}
          onVoiceFileChange={studio.handleVoiceFileChange}
          onVoiceRegister={studio.handleVoiceRegister}
          onVoiceSelect={studio.setSelectedVoiceId}
          publicVoices={studio.publicVoices}
          selectedVoiceId={studio.selectedVoiceId}
          voiceAlias={studio.voiceAlias}
          voiceLoadError={studio.voiceLoadError}
          voiceRegisterError={studio.voiceRegisterError}
          voiceRegistering={studio.voiceRegistering}
          visibilityUpdatingId={studio.voiceVisibilityUpdatingId}
        />
      ) : null}

      {studio.modalState?.type === 'debate-setup' && selectedClone ? (
        <DebateSetupModal
          availableClones={studio.clones}
          availableVoices={studio.voices}
          clone={selectedClone}
          cloneLoadError={studio.cloneLoadError}
          debateOpponent={studio.debateOpponent}
          debateOpponentId={studio.debateOpponentId}
          debateSetupError={studio.debateSetupError}
          debateTopic={studio.debateTopic}
          debateVoiceAId={studio.debateVoiceAId}
          debateVoiceBId={studio.debateVoiceBId}
          onBack={studio.goBackToCloneActions}
          onClose={studio.closeModal}
          onOpponentChange={studio.setDebateOpponentId}
          onStartDebate={studio.handleStartDebate}
          onTopicChange={studio.setDebateTopic}
          onVoiceAChange={studio.setDebateVoiceAId}
          onVoiceBChange={studio.setDebateVoiceBId}
          voiceLoadError={studio.voiceLoadError}
        />
      ) : null}
    </>
  )

  return (
    <SnsShell
      currentUser={currentUser}
      deactivating={deactivating}
      onDeactivate={onDeactivate}
      onLogout={onLogout}
      profileContent={profileWorkspace}
    />
  )
}

export default CloneStudioPage
