import { Input } from '../../../components/ui/Input'

type PeopleSearchBarProps = {
  value: string
  onChange: (value: string) => void
}

export function PeopleSearchBar({ value, onChange }: PeopleSearchBarProps) {
  return (
    <Input
      placeholder="이름이나 아이디로 사람 찾기"
      aria-label="사람 검색"
      value={value}
      onChange={(event) => onChange(event.target.value)}
    />
  )
}
