`import Ember from 'ember'`
`import TopicMatchTree from './topic-match-tree'`

MatchManagementTreeComponent = TopicMatchTree.extend
  classNames: ["topic-management"]
  templateName: "components/topic-match-tree"
  advanced: true

`export default MatchManagementTreeComponent`
