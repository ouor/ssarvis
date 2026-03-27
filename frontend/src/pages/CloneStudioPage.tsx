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

  return (
    <main className="studio-shell">
      <section className="studio-account-bar">
        <div>
          <p className="studio-kicker">Signed In</p>
          <strong>{currentUser.displayName}</strong>
          <span>@{currentUser.username}</span>
        </div>
        <div className="studio-account-actions">
          <button className="secondary-button" disabled={deactivating} onClick={onLogout} type="button">
            로그아웃
          </button>
          <button className="secondary-button" disabled={deactivating} onClick={() => void onDeactivate()} type="button">
            {deactivating ? '탈퇴 처리 중...' : '회원 탈퇴'}
          </button>
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

      {studio.modalState?.type === 'clone-actions' && studio.selectedClone ? (
        <CloneActionsModal
          canManageVisibility={studio.mineClones.some((clone) => clone.cloneId === studio.selectedClone!.cloneId)}
          clone={studio.selectedClone}
          currentUserDisplayName={currentUser.displayName}
          onChatSelect={studio.openVoicePicker}
          onClose={studio.closeModal}
          onDebateSelect={studio.openDebateSetup}
          onToggleVisibility={() => studio.toggleCloneVisibility(studio.selectedClone!)}
          visibilityUpdating={studio.cloneVisibilityUpdatingId === studio.selectedClone!.cloneId}
        />
      ) : null}

      {studio.modalState?.type === 'voice-picker' && studio.selectedClone ? (
        <VoicePickerModal
          clone={studio.selectedClone}
          currentUserDisplayName={currentUser.displayName}
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
          voiceAlias={studio.voiceAlias}
          selectedVoiceId={studio.selectedVoiceId}
          voiceLoadError={studio.voiceLoadError}
          voiceRegisterError={studio.voiceRegisterError}
          voiceRegistering={studio.voiceRegistering}
          visibilityUpdatingId={studio.voiceVisibilityUpdatingId}
        />
      ) : null}

      {studio.modalState?.type === 'debate-setup' && studio.selectedClone ? (
        <DebateSetupModal
          availableClones={studio.clones}
          availableVoices={studio.voices}
          clone={studio.selectedClone}
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
    </main>
  )
}

export default CloneStudioPage
