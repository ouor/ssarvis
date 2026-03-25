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

function CloneStudioPage() {
  const studio = useCloneStudio()

  return (
    <main className="studio-shell">
      <StudioHero activeTab={studio.activeTab} cloneCount={studio.clones.length} voiceCount={studio.voices.length} />
      <StudioTabs activeTab={studio.activeTab} onTabChange={studio.setActiveTab} />

      {studio.activeTab === 'clones' ? (
        <CloneGridSection
          clones={studio.clones}
          loadError={studio.cloneLoadError}
          onCloneSelect={studio.openCloneActions}
          onCreateClone={studio.openCreateCloneModal}
        />
      ) : (
        <LiveSessionPanel
          liveChat={studio.liveChat}
          liveDebate={studio.liveDebate}
          onChatInputChange={studio.handleChatInputChange}
          onChatSubmit={studio.handleChatSubmit}
          onDebateStop={studio.handleDebateStop}
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
          onVoiceFileChange={studio.handleVoiceFileChange}
          onVoiceRegister={studio.handleVoiceRegister}
          onVoiceSelect={studio.setSelectedVoiceId}
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
