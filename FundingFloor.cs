using UnityEngine;

namespace FundingFloor {
  [KSPScenario(ScenarioCreationOptions.AddToNewCareerGames | ScenarioCreationOptions.AddToExistingCareerGames,
    GameScenes.SPACECENTER, GameScenes.EDITOR, GameScenes.FLIGHT, GameScenes.TRACKSTATION)]
  public class FundingFloorScenario : ScenarioModule {
    Settings cfg;

    static void Log(string message) {
      Debug.Log($"[FundingFloor] {message}");
    }

    public void Start() {
      Log("Starting up Funding Floor...");
      cfg = HighLogic.CurrentGame.Parameters.CustomParams<Settings>();

      // GameEvents.onGameNewStart.Add(OnGameNewStart);
      // GameEvents.onGameStatePostLoad.Add(OnGameStatePostLoad);
      GameEvents.OnGameSettingsApplied.Add(OnGameSettingsApplied);

      GameEvents.OnFundsChanged.Add(OnFundsChanged);
      GameEvents.OnReputationChanged.Add(OnReputationChanged);
      GameEvents.OnScienceChanged.Add(OnScienceChanged);

      CalcBudget();
      ClampFunds();

      // GameEvents.OnKSCFacilityUpgraded
      // GameEvents.OnProgressComplete
      // GameEvents.OnTechnologyResearched
      Log("Funding Floor started.");
    }

    public void OnDisable() {
      Log("Shutting down Funding Floor...");

      GameEvents.onGameNewStart.Remove(OnGameNewStart);
      GameEvents.onGameStatePostLoad.Remove(OnGameStatePostLoad);
      GameEvents.OnGameSettingsApplied.Remove(OnGameSettingsApplied);

      GameEvents.OnFundsChanged.Remove(OnFundsChanged);
      GameEvents.OnReputationChanged.Remove(OnReputationChanged);
      GameEvents.OnScienceChanged.Remove(OnScienceChanged);

      Log("Funding Floor shut down.");
    }

    int budget;

    void CalcBudget() {
      double funds_per_rep = (cfg.MaxFunding - cfg.MinFunding)/1000.0;
      budget = (int)System.Math.Max(Reputation.CurrentRep * funds_per_rep, cfg.MinFunding);
      Log($"Budget recalculated as {budget} with {funds_per_rep} funds/rep");
    }

    void ClampFunds() {
      float rep = Reputation.CurrentRep;
      double funds = Funding.Instance.Funds;
      if (cfg.KeepExcessFunding ? (funds >= budget) : (funds == budget)) {
        return;
      }
      Log($"Setting player funds to {budget}");
      Funding.Instance.SetFunds(budget, TransactionReasons.None);
    }

    public void OnGameNewStart() {
      Log($"FF:Start: funds={Funding.Instance.Funds} rep={Reputation.CurrentRep})");
      CalcBudget();
    }

    public void OnGameStatePostLoad(ConfigNode unused) {
      Log($"FF:PostLoad: funds={Funding.Instance.Funds} rep={Reputation.CurrentRep})");
      CalcBudget();
    }

    public void OnGameSettingsApplied() {
      Log($"FF:SettingsApplied: funds={Funding.Instance.Funds} rep={Reputation.CurrentRep})");
      cfg = HighLogic.CurrentGame.Parameters.CustomParams<Settings>();
      CalcBudget();
      ClampFunds();
      // Recalc based on game settings
      // Settings.Instance.FundingPerReputation, etc
    }

    public void OnFundsChanged(double val, TransactionReasons txns) {
      if (Funding.Instance == null) return;

      Log($"FF:OnFundsChanged: funds={Funding.Instance.Funds} new={val}, why={txns})");
      ClampFunds();
    }

    public void OnScienceChanged(float val, TransactionReasons txns) {
      if (ResearchAndDevelopment.Instance == null) return;

      Log($"FF:OnScienceChanged: science={ResearchAndDevelopment.Instance.Science} new={val}, why={txns})");
    }

    public void OnReputationChanged(float val, TransactionReasons txns) {
      if (Reputation.Instance == null) return;

      Log($"FF:OnReputationChanged: rep={Reputation.CurrentRep} new={val}, why={txns})");
      CalcBudget();
      ClampFunds();
    }
  }
}
