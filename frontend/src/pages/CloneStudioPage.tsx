import type { CurrentUser } from '../features/clone-studio/types'
import CloneGridSection from '../features/clone-studio/components/CloneGridSection'
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
  onLogout: () => void
}

function CloneStudioPage({ currentUser, onLogout }: CloneStudioPageProps) {
  const studio = useCloneStudio(currentUser)

  return (
    <main className="studio-shell">
      <section className="studio-account-bar">
        <div>
          <p className="studio-kicker">Signed In</p>
          <strong>{currentUser.displayName}</strong>
          <span>@{currentUser.username}</span>
        </div>
        <button className="secondary-button" onClick={onLogout} type="button">
          로그아웃
        </button>
      </section>
      <StudioHero
        activeTab={studio.activeTab}
        cloneCount={studio.clones.length}
        currentUser={currentUser}
        voiceCount={studio.voices.length}
      />
      <StudioTabs activeTab={studio.activeTab} onTabChange={studio.setActiveTab} />

      {studio.activeTab === 'clones' ? (
        <CloneGridSection
          clones={studio.clones}
          currentUser={currentUser}
          loadError={studio.cloneLoadError}
          onCloneSelect={studio.openCloneActions}
          onCreateClone={studio.openCreateCloneModal}
        />
      ) : (
        <LiveSessionPanel
          currentUser={currentUser}
          liveChat={studio.liveChat}
          liveDebate={studio.liveDebate}
          onChatInputChange={studio.handleChatInputChange}
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
          clone={studio.selectedClone}
          onChatSelect={studio.openVoicePicker}
          onClose={studio.closeModal}
          onDebateSelect={studio.openDebateSetup}
        />
      ) : null}

      {studio.modalState?.type === 'voice-picker' && studio.selectedClone ? (
        <VoicePickerModal
          clone={studio.selectedClone}
          onBack={studio.goBackToCloneActions}
          onClose={studio.closeModal}
          onStartChat={studio.handleStartChat}
          onVoiceAliasChange={studio.setVoiceAlias}
          onVoiceFileChange={studio.handleVoiceFileChange}
          onVoiceRegister={studio.handleVoiceRegister}
          onVoiceSelect={studio.setSelectedVoiceId}
          voiceAlias={studio.voiceAlias}
          selectedVoiceId={studio.selectedVoiceId}
          voiceLoadError={studio.voiceLoadError}
          voiceRegisterError={studio.voiceRegisterError}
          voiceRegistering={studio.voiceRegistering}
          voices={studio.voices}
        />
      ) : null}

      {studio.modalState?.type === 'debate-setup' && studio.selectedClone ? (
        <DebateSetupModal
          clone={studio.selectedClone}
          clones={studio.clones}
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
          voices={studio.voices}
        />
      ) : null}
    </main>
  )
}

export default CloneStudioPage
