const student = {
  id: 4,
  username: 'john@example.com',
  firstName: 'John',
  lastName: 'Smith',
  email: 'john@example.com',
  teamName: 'Team1',
  teamId: 1,
  sectionId: 2,
  enabled: true
}

const result = (data) => ({ flag: true, code: 200, data })

function visitAs(path, userInfo) {
  cy.visit(path, {
    onBeforeLoad(window) {
      const payload = window.btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 600 }))
      window.localStorage.setItem('token', JSON.stringify({ token: `test.${payload}.test` }))
      window.localStorage.setItem('userInfo', JSON.stringify({ userInfo }))
      window.localStorage.setItem('settings', JSON.stringify({ defaultSectionId: 2 }))
    }
  })
}

describe('Student deactivation', () => {
  beforeEach(() => {
    let enabled = true
    cy.intercept('POST', '**/students/search*', (request) => {
      request.reply(
        result({ content: [{ ...student, enabled }], totalElements: 1, number: 0, size: 50 })
      )
    }).as('roster')
    cy.intercept('GET', '**/students/4', (request) =>
      request.reply(result({ ...student, enabled }))
    )
    cy.intercept('POST', '**/students/4/deactivate', (request) => {
      enabled = false
      request.reply(result({ ...student, enabled }))
    }).as('deactivate')

    visitAs('/students', { id: 2, firstName: 'Bill', lastName: 'Gates', roles: 'instructor' })
    cy.wait('@roster')
    cy.contains('tr', 'john@example.com').find('button').click()
    cy.contains('button', 'Deactivate Student').click()
  })

  it('closes the warning without sending a request when canceled (STU-08)', () => {
    cy.get('.el-message-box').should('contain', 'stop receiving reminders')
    cy.get('.el-message-box').contains('button', 'Cancel').click()
    cy.get('.el-message-box').should('not.exist')
    cy.get('.el-dialog').should('be.visible').and('contain', 'Enabled')
    cy.get('@deactivate.all').should('have.length', 0)
  })

  it('sends one request after confirmation and refreshes the visible status (STU-01)', () => {
    cy.get('.el-message-box').screenshot('deactivation-warning')
    cy.get('.el-message-box').should('contain', 'existing peer evaluations will be kept')
    cy.get('.el-message-box')
      .contains('button', /^Deactivate$/)
      .click()
    cy.wait('@deactivate')
    cy.wait('@roster')
    cy.get('.el-dialog').should('contain', 'Deactivated')
    cy.contains('tr', 'john@example.com').should('contain', 'Disabled')
    cy.get('@deactivate.all').should('have.length', 1)
  })
})

it('omits inactive teammates from a new peer evaluation form (STU-03)', () => {
  const inactive = { ...student, id: 5, firstName: 'Eric', lastName: 'Hudson', enabled: false }
  cy.intercept('GET', '**/students/teams/1', { body: result([student, inactive]) })
  cy.intercept('GET', '**/sections/2', { body: result({ rubricId: 1 }) })
  cy.intercept('GET', '**/rubrics/1', {
    body: result({ criteria: [{ criterionId: 1, criterion: 'Quality of work', maxScore: 10 }] })
  })
  cy.intercept('GET', '**/evaluations/evaluators/4/week/*', { body: result([]) }).as('evaluations')
  visitAs('/evaluations/submit-evaluations', { ...student, roles: 'student' })
  cy.wait('@evaluations')
  cy.contains('John Smith').should('be.visible')
  cy.contains('Eric Hudson').should('not.exist')
})
